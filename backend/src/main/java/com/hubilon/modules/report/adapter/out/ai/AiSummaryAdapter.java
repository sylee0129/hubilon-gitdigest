package com.hubilon.modules.report.adapter.out.ai;

import com.hubilon.modules.report.domain.model.AiSummaryResult;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
                    return new AiSummaryResult(text.trim(), true);
                }
            }

            log.warn("Gemini API 응답에서 텍스트를 추출하지 못했습니다. stub으로 대체합니다.");
            return new AiSummaryResult(stubSummarize(commits), false);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("Gemini API 호출 실패 (status={}). stub으로 대체합니다.", e.getStatusCode().value());
            return new AiSummaryResult(stubSummarize(commits), false);
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생. stub으로 대체합니다.", e);
            return new AiSummaryResult(stubSummarize(commits), false);
        }
    }
}
