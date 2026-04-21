package com.hubilon.modules.report.adapter.out.gitlab;

import com.hubilon.common.exception.custom.ExternalServiceException;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.port.out.GitProviderAdapter;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FileChange;
import com.hubilon.modules.report.domain.port.out.GitCommitPort;
import com.hubilon.modules.report.domain.port.out.GitLabPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabAdapter implements GitLabPort, GitCommitPort, GitProviderAdapter {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_DATE_TIME;

    private final WebClient gitlabWebClient;

    @Override
    public List<CommitInfo> fetchCommits(Long gitlabProjectId, String gitlabBaseUrl, String accessToken, String authType,
                                          LocalDate startDate, LocalDate endDate) {
        try {
            String since = startDate.atStartOfDay().format(ISO_FMT);
            String until = endDate.plusDays(1).atStartOfDay().format(ISO_FMT);
            String trimmedToken = trim(accessToken);

            log.info("fetchCommits: projectId={}, baseUrl={}, authType={}, tokenPresent={}",
                    gitlabProjectId, gitlabBaseUrl, authType, !trimmedToken.isEmpty());

            WebClient client = buildClient(gitlabBaseUrl);

            List<GitLabCommitResponse> commits = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v4/projects/{projectId}/repository/commits")
                            .queryParam("since", since)
                            .queryParam("until", until)
                            .queryParam("per_page", 100)
                            .build(gitlabProjectId))
                    .headers(authHeaders(trimmedToken, authType))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitLabCommitResponse>>() {})
                    .block();

            if (commits == null) return Collections.emptyList();

            return commits.stream()
                    .map(commit -> {
                        List<FileChange> fileChanges = fetchDiff(client, gitlabProjectId, trimmedToken, authType, commit.id());
                        return CommitInfo.builder()
                                .sha(commit.id())
                                .authorName(commit.authorName())
                                .authorEmail(commit.authorEmail())
                                .committedAt(parseDateTime(commit.committedDate()))
                                .message(commit.message())
                                .fileChanges(fileChanges)
                                .build();
                    })
                    .toList();

        } catch (WebClientResponseException e) {
            log.warn("GitLab API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("GitLab API 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private List<FileChange> fetchDiff(WebClient client, Long gitlabProjectId, String accessToken, String authType, String sha) {
        try {
            List<GitLabDiffResponse> diffs = client.get()
                    .uri("/api/v4/projects/{projectId}/repository/commits/{sha}/diff", gitlabProjectId, sha)
                    .headers(authHeaders(accessToken, authType))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitLabDiffResponse>>() {})
                    .block();

            if (diffs == null) return Collections.emptyList();

            return diffs.stream()
                    .map(diff -> {
                        int[] lines = countDiffLines(diff.diff());
                        return FileChange.builder()
                                .oldPath(diff.oldPath())
                                .newPath(diff.newPath())
                                .newFile(diff.newFile())
                                .renamedFile(diff.renamedFile())
                                .deletedFile(diff.deletedFile())
                                .addedLines(lines[0])
                                .removedLines(lines[1])
                                .build();
                    })
                    .toList();

        } catch (WebClientResponseException e) {
            log.warn("GitLab diff API error for sha={}: {}", sha, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** gitlabUrl(전체 clone URL 또는 base URL)에서 scheme+host만 추출하여 WebClient를 생성한다. */
    private WebClient buildClient(String gitlabUrl) {
        if (gitlabUrl == null || gitlabUrl.isBlank()) {
            return gitlabWebClient;
        }
        try {
            URI uri = URI.create(gitlabUrl);
            String baseUrl = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            return gitlabWebClient.mutate().baseUrl(baseUrl).build();
        } catch (Exception e) {
            log.warn("Invalid gitlabUrl '{}', falling back to default client", gitlabUrl);
            return gitlabWebClient;
        }
    }

    // --- GitProviderAdapter 인터페이스 구현 ---

    @Override
    public Long resolveProjectId(String repoUrl, String token) {
        String projectPath = extractProjectPath(repoUrl);
        return resolveProjectId(projectPath, token, "OAUTH");
    }

    @Override
    public String resolveProjectName(String repoUrl, String token) {
        String projectPath = extractProjectPath(repoUrl);
        return resolveProjectName(projectPath, token, "OAUTH");
    }

    @Override
    public GitProvider supports() {
        return GitProvider.GITLAB;
    }

    private String extractProjectPath(String gitlabUrl) {
        String[] parts = gitlabUrl.split("/", -1);
        int hostParts = 3; // https:, "", gitlab.com
        if (parts.length <= hostParts) {
            return gitlabUrl;
        }
        StringBuilder path = new StringBuilder();
        for (int i = hostParts; i < parts.length; i++) {
            if (i > hostParts) path.append("/");
            path.append(parts[i]);
        }
        String result = path.toString();
        if (result.endsWith(".git")) {
            result = result.substring(0, result.length() - 4);
        }
        return result;
    }

    // --- 기존 메서드 (authType 포함 오버로드) ---

    public Long resolveProjectId(String projectPath, String accessToken, String authType) {
        try {
            GitLabProjectResponse project = fetchProject(projectPath, accessToken, authType);
            if (project == null) {
                throw new ExternalServiceException("GitLab 프로젝트를 찾을 수 없습니다: " + projectPath);
            }
            return project.id();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("GitLab project resolve error: path={}, status={}", projectPath, e.getStatusCode());
            throw new ExternalServiceException("GitLab 프로젝트 조회 중 오류가 발생했습니다: " + e.getStatusCode(), e);
        }
    }

    public String resolveProjectName(String projectPath, String accessToken, String authType) {
        try {
            GitLabProjectResponse project = fetchProject(projectPath, accessToken, authType);
            return project != null ? project.name() : projectPath;
        } catch (ExternalServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.warn("GitLab project name resolve error: {}", e.getMessage());
            return projectPath;
        }
    }

    private GitLabProjectResponse fetchProject(String projectPath, String accessToken, String authType) {
        String trimmedToken = trim(accessToken);
        boolean hasToken = !trimmedToken.isEmpty();

        if (hasToken) {
            try {
                return gitlabWebClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/api/v4/projects/{path}").build(projectPath))
                        .headers(authHeaders(trimmedToken, authType))
                        .retrieve()
                        .bodyToMono(GitLabProjectResponse.class)
                        .block();
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().value() != 401 && e.getStatusCode().value() != 403) throw e;
                log.warn("GitLab auth failed ({}), retrying without token: {}", e.getStatusCode().value(), projectPath);
            }
        }

        // 퍼블릭 프로젝트 fallback
        try {
            return gitlabWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v4/projects/{path}").build(projectPath))
                    .retrieve()
                    .bodyToMono(GitLabProjectResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 404) {
                String guide = hasToken
                        ? "토큰이 유효하지 않거나 read_api / api 스코프가 없습니다."
                        : "프라이빗 프로젝트입니다. Personal Access Token을 입력해 주세요.";
                throw new ExternalServiceException(guide, e);
            }
            throw e;
        }
    }

    /**
     * 토큰이 있으면 인증 헤더를 설정하는 Consumer 반환.
     * headers(Consumer) 방식으로 와일드카드 타입 문제를 회피한다.
     */
    private Consumer<HttpHeaders> authHeaders(String accessToken, String authType) {
        String trimmed = trim(accessToken);
        if (trimmed.isEmpty()) return headers -> {};
        String headerName = "OAUTH".equalsIgnoreCase(authType) ? "Authorization" : "PRIVATE-TOKEN";
        String headerValue = "OAUTH".equalsIgnoreCase(authType) ? "Bearer " + trimmed : trimmed;
        return headers -> headers.set(headerName, headerValue);
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null) return null;
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }

    private int[] countDiffLines(String diff) {
        if (diff == null) return new int[]{0, 0};
        int added = 0, removed = 0;
        for (String line : diff.split("\n")) {
            if (line.startsWith("+") && !line.startsWith("+++")) added++;
            else if (line.startsWith("-") && !line.startsWith("---")) removed++;
        }
        return new int[]{added, removed};
    }
}
