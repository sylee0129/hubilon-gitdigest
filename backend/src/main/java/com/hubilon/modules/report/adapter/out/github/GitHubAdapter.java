package com.hubilon.modules.report.adapter.out.github;

import com.hubilon.common.exception.custom.ExternalServiceException;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.port.out.GitProviderAdapter;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FileChange;
import com.hubilon.modules.report.domain.port.out.GitCommitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubAdapter implements GitProviderAdapter, GitCommitPort {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final WebClient.Builder webClientBuilder;

    @Override
    public Long resolveProjectId(String repoUrl, String token) {
        GitHubRepoResponse repo = fetchRepo(repoUrl, token);
        return repo.id();
    }

    @Override
    public String resolveProjectName(String repoUrl, String token) {
        GitHubRepoResponse repo = fetchRepo(repoUrl, token);
        return repo.name();
    }

    @Override
    public GitProvider supports() {
        return GitProvider.GITHUB;
    }

    private GitHubRepoResponse fetchRepo(String repoUrl, String token) {
        String ownerRepo = extractOwnerRepo(repoUrl);
        try {
            WebClient client = webClientBuilder.baseUrl(GITHUB_API_BASE).build();
            return client.get()
                    .uri("/repos/" + ownerRepo)
                    .headers(headers -> {
                        if (token != null && !token.isBlank()) {
                            headers.set("Authorization", "Bearer " + token.trim());
                        }
                        headers.set("Accept", "application/vnd.github+json");
                        headers.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(GitHubRepoResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
//            log.warn("GitHub API error: ownerRepo={}, status={}", ownerRepo, e.getStatusCode());
            log.warn("GitHub API error: ownerRepo={}, status={}, body={}",
                             ownerRepo, e.getStatusCode(), e.getResponseBodyAsString()); // 상세 바디 로그 추가
            throw new ExternalServiceException("GitHub 프로젝트 조회 중 오류가 발생했습니다: " + e.getStatusCode(), e);
        }
    }

    // --- GitCommitPort 구현 ---

    @Override
    public List<CommitInfo> fetchCommits(Long projectNumericId, String repoUrl, String accessToken, String authType,
                                          LocalDate startDate, LocalDate endDate) {
        String ownerRepo = extractOwnerRepo(repoUrl);
        String since = startDate.atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
        String until = endDate.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_DATE_TIME) + "Z";

        try {
            WebClient client = webClientBuilder.baseUrl(GITHUB_API_BASE).build();

            List<GitHubCommitListItem> commits = client.get()
                    .uri("/repos/" + ownerRepo + "/commits?since=" + since + "&until=" + until + "&per_page=100")
                    .headers(h -> {
                        if (accessToken != null && !accessToken.isBlank()) {
                            h.set("Authorization", "Bearer " + accessToken.trim());
                        }
                        h.set("Accept", "application/vnd.github+json");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GitHubCommitListItem>>() {})
                    .block();

            if (commits == null) return Collections.emptyList();

            return commits.stream()
                    .map(item -> {
                        List<FileChange> fileChanges = fetchGitHubDiff(client, ownerRepo, item.sha(), accessToken);
                        GitHubCommitListItem.AuthorData author = item.commit() != null ? item.commit().author() : null;
                        return CommitInfo.builder()
                                .sha(item.sha())
                                .authorName(author != null ? author.name() : null)
                                .authorEmail(author != null ? author.email() : null)
                                .committedAt(author != null ? parseGitHubDate(author.date()) : null)
                                .message(item.commit() != null ? item.commit().message() : null)
                                .fileChanges(fileChanges)
                                .build();
                    })
                    .toList();

        } catch (WebClientResponseException e) {
            log.warn("GitHub commits API error: ownerRepo={}, status={}", ownerRepo, e.getStatusCode());
            throw new ExternalServiceException("GitHub 커밋 조회 중 오류가 발생했습니다: " + e.getStatusCode(), e);
        }
    }

    private List<FileChange> fetchGitHubDiff(WebClient client, String ownerRepo, String sha, String accessToken) {
        try {
            GitHubCommitDetail detail = client.get()
                    .uri("/repos/" + ownerRepo + "/commits/" + sha)
                    .headers(h -> {
                        if (accessToken != null && !accessToken.isBlank()) {
                            h.set("Authorization", "Bearer " + accessToken.trim());
                        }
                        h.set("Accept", "application/vnd.github+json");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(GitHubCommitDetail.class)
                    .block();

            if (detail == null || detail.files() == null) return Collections.emptyList();

            return detail.files().stream()
                    .map(f -> FileChange.builder()
                            .oldPath(f.previousFilename() != null ? f.previousFilename() : f.filename())
                            .newPath(f.filename())
                            .newFile("added".equals(f.status()))
                            .renamedFile("renamed".equals(f.status()))
                            .deletedFile("removed".equals(f.status()))
                            .addedLines(f.additions())
                            .removedLines(f.deletions())
                            .build())
                    .toList();

        } catch (WebClientResponseException e) {
            log.warn("GitHub commit detail API error: sha={}, status={}", sha, e.getStatusCode());
            return Collections.emptyList();
        }
    }

    private LocalDateTime parseGitHubDate(String date) {
        if (date == null) return null;
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse GitHub date: {}", date);
            return null;
        }
    }

    /**
     * https://github.com/owner/repo(.git) → owner/repo
     */
    private String extractOwnerRepo(String repoUrl) {
        if (repoUrl == null || !repoUrl.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("GitHub URL 형식이 올바르지 않습니다. (예: https://github.com/owner/repo)");
        }
        String path = repoUrl.substring("https://github.com/".length());
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        String[] parts = path.split("/");
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("GitHub URL에서 owner/repo를 추출할 수 없습니다: " + repoUrl);
        }
        return parts[0] + "/" + parts[1];
    }
}
