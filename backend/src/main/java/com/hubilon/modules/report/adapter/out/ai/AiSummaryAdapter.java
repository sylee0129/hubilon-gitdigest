package com.hubilon.modules.report.adapter.out.ai;

import com.hubilon.modules.report.domain.model.AiSummaryResult;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiSummaryAdapter implements AiSummaryPort {

    @Value("${ai.summary.enabled:false}")
    private boolean aiEnabled;

    @Value("${ai.summary.api-key:}")
    private String apiKey;

    @Value("${ai.summary.model:gemini-2.0-flash-lite}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    @Override
    public AiSummaryResult summarize(List<CommitInfo> commits) {
        if (commits == null || commits.isEmpty()) {
            return new AiSummaryResult("이 기간에 커밋이 없습니다.", true);
        }

        if (aiEnabled) {
            return callLlmApi(commits);
        }

        return new AiSummaryResult(stubSummarize(commits), false);
    }

    @Override
    public FolderAiSummaryResult summarizeFolder(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate, String folderName) {
        int commitCount = commits == null ? 0 : commits.size();
        log.info("[AI요약] 시작: folderName={}, commitCount={}, aiEnabled={}", folderName, commitCount, aiEnabled);

        if (commits == null || commits.isEmpty()) {
            log.info("[AI요약] 커밋 없음 → 빈 값 반환: folderName={}", folderName);
            return new FolderAiSummaryResult("", "", false);
        }

        if (aiEnabled) {
            log.info("[AI요약] Gemini API 호출: folderName={}, model={}", folderName, model);
            FolderAiSummaryResult result = callFolderLlmApi(commits, startDate, endDate, folderName);
            log.info("[AI요약] Gemini 완료: folderName={}, aiUsed={}", folderName, result.aiUsed());
            return result;
        }

        log.info("[AI요약] aiEnabled=false → stub 사용: folderName={}", folderName);
        return stubSummarizeFolder(commits, folderName);
    }

    // ─── private helpers ────────────────────────────────────────────────────────

    private String stubSummarize(List<CommitInfo> commits) {
        log.debug("Using stub AI summary for {} commits", commits.size());

        Map<String, List<CommitInfo>> byAuthor = commits.stream()
                .collect(Collectors.groupingBy(c ->
                        c.getAuthorName() != null ? c.getAuthorName() : "Unknown",
                        java.util.LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        byAuthor.forEach((author, authorCommits) -> {
            String summary = authorCommits.stream()
                    .map(c -> c.getMessage() != null
                            ? c.getMessage().trim().lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("(메시지 없음)")
                            : "(메시지 없음)")
                    .collect(Collectors.joining(", "));
            sb.append(author).append(": ").append(summary).append("\n");
        });

        return sb.toString().trim();
    }

    private FolderAiSummaryResult stubSummarizeFolder(List<CommitInfo> commits, String folderName) {
        String progress = "- " + folderName + "\n    > 개발\n    : " + stubSummarize(commits);
        String plan = "- " + folderName + "\n    > (stub) 차주 계획 자동 생성 불가 — 직접 입력해 주세요.";
        return new FolderAiSummaryResult(progress, plan, false);
    }

    private AiSummaryResult callLlmApi(List<CommitInfo> commits) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ai.summary.api-key가 설정되지 않았습니다. stub으로 대체합니다.");
            return new AiSummaryResult(stubSummarize(commits), false);
        }

        Map<String, List<CommitInfo>> byAuthor = commits.stream()
                .collect(Collectors.groupingBy(c -> {
                    String name = c.getAuthorName() != null ? c.getAuthorName() : "Unknown";
                    String email = c.getAuthorEmail() != null ? c.getAuthorEmail() : "";
                    return email.isBlank() ? name : name + " <" + email + ">";
                }, java.util.LinkedHashMap::new, Collectors.toList()));

        StringBuilder commitSection = new StringBuilder();
        byAuthor.forEach((author, authorCommits) -> {
            commitSection.append("[").append(author).append("]\n");
            authorCommits.forEach(c -> {
                String msg = c.getMessage() != null
                        ? c.getMessage().trim().lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("(메시지 없음)")
                        : "(메시지 없음)";
                commitSection.append("- ").append(msg).append("\n");
            });
            commitSection.append("\n");
        });

        String prompt = """
                다음은 담당자별 Git 커밋 목록입니다.
                각 담당자의 이름과 작업 내용만 아래 형식으로 간결하게 요약해주세요.
                설명 문구, 머리말, 꼬리말 없이 형식만 출력하세요.

                출력 형식:
                {이름}: {작업 내용 핵심 요약 1~2줄}

                [커밋 목록]
                %s
                """.formatted(commitSection.toString());

        String text = callGemini(prompt);
        if (text != null) {
            return new AiSummaryResult(text.trim(), true);
        }

        log.warn("Gemini API 응답에서 텍스트를 추출하지 못했습니다. stub으로 대체합니다.");
        return new AiSummaryResult(stubSummarize(commits), false);
    }

    private FolderAiSummaryResult callFolderLlmApi(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate, String folderName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ai.summary.api-key가 설정되지 않았습니다. stub으로 대체합니다.");
            return stubSummarizeFolder(commits, folderName);
        }

        String startMD = startDate.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
        String endMD = endDate.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
        String nextStartMD = endDate.plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));
        String nextEndMD = endDate.plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));

        Map<String, List<CommitInfo>> byAuthor = commits.stream()
                .collect(Collectors.groupingBy(c -> {
                    String name = c.getAuthorName() != null ? c.getAuthorName() : "Unknown";
                    String email = c.getAuthorEmail() != null ? c.getAuthorEmail() : "";
                    return email.isBlank() ? name : name + " <" + email + ">";
                }, java.util.LinkedHashMap::new, Collectors.toList()));

        StringBuilder commitSection = new StringBuilder();
        byAuthor.forEach((author, authorCommits) -> {
            commitSection.append("[").append(author).append("]\n");
            authorCommits.forEach(c -> {
                String msg = c.getMessage() != null
                        ? c.getMessage().trim().lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("(메시지 없음)")
                        : "(메시지 없음)";
                commitSection.append("- ").append(msg).append("\n");
            });
            commitSection.append("\n");
        });

        String prompt = String.format(
                """
                ## Role
                너는 소프트웨어 개발팀의 주간 업무 보고서를 작성하는 전문 테크니컬 라이터이자 프로젝트 관리자야.
                전달받은 [커밋 이력]을 바탕으로, 읽는 사람이 한눈에 성과를 파악할 수 있도록 보고서를 작성해 줘.
                프로젝트명: %s

                ## Writing Guidelines
                - **카테고리 분류**: 커밋 메시지를 분석하여 아래 섹션 중 해당하는 항목에만 내용을 채워줘. 해당 사항 없는 섹션은 제외.
                    1. [신규 기능 개발] — 새로운 기능 추가 (feat, feature 키워드)
                    2. [성능 및 UI 개선] — 기존 기능 개선, 리팩터링 (refactor, improve, enhance 키워드)
                    3. [결함 수정 및 안정화] — 버그 수정 (fix, bug, hotfix 키워드)
                    4. [인프라/DB/배포] — 빌드, 배포, 설정 변경 (ci, deploy, config, build 키워드)
                    5. [문서화 작업] — 문서 및 주석 (docs, readme, comment 키워드)
                    6. [유지보수] — 빌드 설정, 의존성, 환경 정리 등 기능 외 작업 (chore 키워드)
                    7. [기타] — 위 분류에 해당하지 않는 기타 작업 (etc 키워드)
                - **문체**: 개조식을 사용하고, "~함", "~완료", "~구현"과 같은 명사형 종결 어미를 사용해.
                - **내용 정제**:
                    - 단순 파일 경로 수정이나 빌드 로그 같은 노이즈는 제외해.
                    - 기술 용어는 유지하되, 비즈니스 가치가 드러나도록 요약해 (예: "버튼 추가" → "사용자 편의성 향상을 위한 인터페이스 개선").
                - **차주 계획**: 금주 진행 사항 중 WIP이거나 검증 예정인 항목을 바탕으로 논리적인 다음 단계를 제안해 줘.
                - **금지**: 인사말, 설명, 머리말/꼬리말 절대 포함 금지 — Output Format만 출력

                ## Output Format
                ### 금주 진행 사항 (%s~%s)
                **[섹션 명칭]**
                - **주요 작업 제목**
                  - 세부 구현 내용 1
                  - 세부 구현 내용 2

                ### 차주 진행 계획 (%s~%s)
                - 금주 개발 기능에 대한 통합 테스트 및 안정화 진행
                - (커밋 패턴 기반으로 추가될 예상 작업 기재)

                [커밋 이력]
                %s
                """,
                folderName, startMD, endMD, nextStartMD, nextEndMD, commitSection.toString());

        String text = callGemini(prompt);
        if (text != null) {
            return parseFolderSummary(text);
        }

        log.warn("Gemini API 응답에서 텍스트를 추출하지 못했습니다. stub으로 대체합니다.");
        return stubSummarizeFolder(commits, folderName);
    }

    private String callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            Map response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + model + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("content-type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(25))
                    .block();

            if (response != null && response.get("candidates") instanceof List<?> candidates && !candidates.isEmpty()) {
                Object first = candidates.get(0);
                if (first instanceof Map<?, ?> candidate
                        && candidate.get("content") instanceof Map<?, ?> content
                        && content.get("parts") instanceof List<?> parts
                        && !parts.isEmpty()
                        && parts.get(0) instanceof Map<?, ?> part
                        && part.get("text") instanceof String text) {
                    return text.trim();
                }
            }

            return null;

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("Gemini API 호출 실패 (status={}). stub으로 대체합니다.", e.getStatusCode().value());
            return null;
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생. stub으로 대체합니다.", e);
            return null;
        }
    }

    private FolderAiSummaryResult parseFolderSummary(String raw) {
        String[] lines = raw.split("\n", -1);

        int progressStart = -1;
        int planStart = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("금주 진행 사항") && progressStart == -1) {
                progressStart = i;
            } else if (line.contains("차주 진행 계획") && planStart == -1) {
                planStart = i;
            }
        }

        // fallback: 구 포맷 지원
        if (progressStart == -1) {
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("[금주 진행사항") && progressStart == -1) {
                    progressStart = i;
                } else if (lines[i].startsWith("[차주 진행계획") && planStart == -1) {
                    planStart = i;
                }
            }
        }

        if (progressStart == -1) {
            return new FolderAiSummaryResult(sanitize(raw), "(자동 추론 불가)", true);
        }

        String progressRaw;
        String planRaw;

        if (planStart != -1 && planStart > progressStart) {
            progressRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, progressStart + 1, planStart)).stripTrailing();
            planRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, planStart + 1, lines.length)).stripTrailing();
        } else {
            progressRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, progressStart + 1, lines.length)).stripTrailing();
            planRaw = "(자동 추론 불가)";
        }

        return new FolderAiSummaryResult(sanitize(progressRaw), sanitize(planRaw), true);
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "").trim();
    }
}
