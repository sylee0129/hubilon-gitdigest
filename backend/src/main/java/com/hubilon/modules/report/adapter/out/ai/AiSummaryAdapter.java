package com.hubilon.modules.report.adapter.out.ai;

import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 요약 어댑터.
 * ai.summary.enabled=false 이면 커밋 메시지를 "• {message}" 형태로 조합하는 stub 구현을 사용합니다.
 * ai.summary.enabled=true 이면 Anthropic API를 호출하여 커밋 내용을 분석·요약합니다.
 */
@Slf4j
@Component
public class AiSummaryAdapter implements AiSummaryPort {

    @Value("${ai.summary.enabled:false}")
    private boolean aiEnabled;

    @Value("${ai.summary.api-key:}")
    private String apiKey;

    @Value("${ai.summary.model:claude-haiku-4-5-20251001}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    @Override
    public String summarize(List<CommitInfo> commits) {
        if (commits == null || commits.isEmpty()) {
            return "이 기간에 커밋이 없습니다.";
        }

        if (aiEnabled) {
            return callLlmApi(commits);
        }

        return stubSummarize(commits);
    }

    private String stubSummarize(List<CommitInfo> commits) {
        log.debug("Using stub AI summary for {} commits", commits.size());

        // 기여자별 그룹핑
        Map<String, List<CommitInfo>> byAuthor = commits.stream()
                .collect(Collectors.groupingBy(c -> {
                    String name = c.getAuthorName() != null ? c.getAuthorName() : "Unknown";
                    String email = c.getAuthorEmail() != null ? c.getAuthorEmail() : "";
                    return email.isBlank() ? name : name + " <" + email + ">";
                }, java.util.LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        byAuthor.forEach((author, authorCommits) -> {
            sb.append("[").append(author).append("]\n");
            for (CommitInfo commit : authorCommits) {
                String msg = commit.getMessage() != null
                        ? commit.getMessage().trim().lines()
                                .map(String::trim)
                                .filter(line -> !line.isEmpty())
                                .findFirst()
                                .orElse("(메시지 없음)")
                        : "(메시지 없음)";
                sb.append("  • ").append(msg).append("\n");
            }
            sb.append("\n");
        });

        return sb.toString().trim();
    }

    private String callLlmApi(List<CommitInfo> commits) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ai.summary.api-key가 설정되지 않았습니다. stub으로 대체합니다.");
            return stubSummarize(commits);
        }

        String commitList = commits.stream()
                .map(c -> "- " + (c.getMessage() != null
                        ? c.getMessage().trim().lines().map(String::trim).filter(l -> !l.isEmpty()).findFirst().orElse("(메시지 없음)")
                        : "(메시지 없음)"))
                .collect(Collectors.joining("\n"));

        String prompt = """
                다음은 이번 주 Git 커밋 메시지 목록입니다. 각 커밋을 분석하여 어떤 작업이 이루어졌는지 한국어로 간결하게 요약해주세요.
                기능 개발, 버그 수정, 리팩토링 등 주요 작업 내용을 중심으로 정리하되, 불필요한 설명 없이 핵심만 작성해주세요.

                [커밋 목록]
                %s
                """.formatted(commitList);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        int[] retryDelaysSeconds = {5, 15, 30};
        for (int attempt = 0; attempt <= retryDelaysSeconds.length; attempt++) {
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

                log.warn("Gemini API 응답에서 텍스트를 추출하지 못했습니다. stub으로 대체합니다.");
                return stubSummarize(commits);

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                if (e.getStatusCode().value() == 429 && attempt < retryDelaysSeconds.length) {
                    log.warn("Gemini API 429 Too Many Requests. {}초 후 재시도 ({}/{})", retryDelaysSeconds[attempt], attempt + 1, retryDelaysSeconds.length);
                    try { Thread.sleep(retryDelaysSeconds[attempt] * 1000L); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("Gemini API 호출 중 오류 발생 (status={}). stub으로 대체합니다.", e.getStatusCode().value(), e);
                    return stubSummarize(commits);
                }
            } catch (Exception e) {
                log.error("Gemini API 호출 중 오류 발생. stub으로 대체합니다.", e);
                return stubSummarize(commits);
            }
        }
        return stubSummarize(commits);
    }
}
