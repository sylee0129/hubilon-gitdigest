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
        if (commits == null || commits.isEmpty()) {
            return new FolderAiSummaryResult("진행사항 없음", "진행사항 확인", true);
        }

        if (aiEnabled) {
            return callFolderLlmApi(commits, startDate, endDate, folderName);
        }

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
                당신은 전문적인 IT 프로젝트 매니저(PM)이자 주간보고서 작성 전문가입니다.
                제공된 [커밋 목록]을 분석하여, 실무 엑셀 보고서의 작성 스타일과 완벽히 일치하는 보고서를 작성하세요.

                [작성 가이드라인]
                1. 문체: 반드시 "~함", "~완료", "~예정"으로 끝나는 격식 있는 개조식 문체 사용 (예: '수정 완료', '계획 수립', '검토 진행')
                2. 구조화: 커밋을 '사업관리', '개발', 'QA/테스트' 카테고리로 논리적으로 분류
                3. 상세도: 커밋 메시지를 나열하지 말고 비즈니스 관점에서 어떤 기능이 완성되었는지 요약
                4. 차주 계획 추론:
                   - 개발 커밋 비중이 높으면 → 통합 테스트 및 버그 수정 예정
                   - 기획/설계 커밋 비중이 높으면 → 개발 환경 구축 및 퍼블리싱 착수 예정
                   - 배포/운영 커밋이 있으면 → 운영 안정화 및 모니터링 예정
                   - 보안/인증 관련 커밋이 있으면 → 보안성 검토 및 취약점 점검 예정
                   금주 커밋 패턴을 분석하여 PM 관점에서 논리적인 다음 단계를 제안
                5. 금지: 인사말, 설명, 머리말/꼬리말 절대 포함 금지 — [출력 형식]만 출력

                [출력 형식]
                [금주 진행사항 (%s~%s)]
                - %s
                    > 사업관리
                    : 내용 (없으면 이 줄 생략)
                    > 개발
                    : 내용 (없으면 이 줄 생략)
                    > QA/테스트
                    : 내용 (없으면 이 줄 생략)

                [차주 진행계획 (%s~%s)]
                - %s
                    > 내용 (금주 작업과 이어지는 논리적 단계)

                [커밋 목록]
                %s
                """,
                startMD, endMD, folderName, nextStartMD, nextEndMD, folderName, commitSection.toString());

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
            if (lines[i].startsWith("[금주 진행사항") && progressStart == -1) {
                progressStart = i;
            } else if (lines[i].startsWith("[차주 진행계획") && planStart == -1) {
                planStart = i;
            }
        }

        if (progressStart == -1) {
            // 구분자를 찾지 못한 경우
            return new FolderAiSummaryResult(sanitize(raw), "(자동 추론 불가)", true);
        }

        String progressRaw;
        String planRaw;

        if (planStart != -1 && planStart > progressStart) {
            progressRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, progressStart, planStart)).stripTrailing();
            planRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, planStart, lines.length)).stripTrailing();
        } else {
            progressRaw = String.join("\n", java.util.Arrays.copyOfRange(lines, progressStart, lines.length)).stripTrailing();
            planRaw = "(자동 추론 불가)";
        }

        return new FolderAiSummaryResult(sanitize(progressRaw), sanitize(planRaw), true);
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "").trim();
    }
}
