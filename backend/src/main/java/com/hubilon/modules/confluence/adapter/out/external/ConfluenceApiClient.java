package com.hubilon.modules.confluence.adapter.out.external;

import com.hubilon.common.exception.custom.ExternalServiceException;
import com.hubilon.modules.confluence.config.ConfluenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceApiClient {

    public record ConfluencePage(String id, int version, String url) {}

    private final ConfluenceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient buildClient() {
        String credentials = properties.userEmail() + ":" + properties.apiToken();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Optional<ConfluencePage> findPageByTitle(String spaceKey, String title) {
        try {
            RestClient client = buildClient();
            String response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/wiki/rest/api/content")
                            .queryParam("spaceKey", spaceKey)
                            .queryParam("title", title)
                            .queryParam("expand", "version")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            if (results.isEmpty()) {
                return Optional.empty();
            }

            JsonNode page = results.get(0);
            String id = page.path("id").asText();
            int version = page.path("version").path("number").asInt();
            String url = properties.baseUrl() + "/wiki/spaces/" + spaceKey + "/pages/" + id;

            return Optional.of(new ConfluencePage(id, version, url));

        } catch (HttpClientErrorException e) {
            log.warn("Confluence findPageByTitle error: title={}, status={}", title, e.getStatusCode());
            throw new ExternalServiceException("Confluence 페이지 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Confluence findPageByTitle parse error: {}", e.getMessage());
            throw new ExternalServiceException("Confluence 응답 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    public String createPage(String spaceKey, String parentId, String title, String body) {
        try {
            RestClient client = buildClient();
            Map<String, Object> requestBody = Map.of(
                    "type", "page",
                    "title", title,
                    "space", Map.of("key", spaceKey),
                    "ancestors", new Object[]{Map.of("id", parentId)},
                    "body", Map.of(
                            "storage", Map.of(
                                    "value", body,
                                    "representation", "storage"
                            )
                    )
            );

            String response = client.post()
                    .uri("/wiki/rest/api/content")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String id = root.path("id").asText();
            return properties.baseUrl() + "/wiki/spaces/" + spaceKey + "/pages/" + id;

        } catch (HttpClientErrorException e) {
            log.warn("Confluence createPage error: title={}, status={}, body={}", title, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Confluence 페이지 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Confluence createPage error: {}", e.getMessage());
            throw new ExternalServiceException("Confluence 페이지 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    public String updatePage(String id, int newVersion, String title, String body) {
        try {
            RestClient client = buildClient();
            Map<String, Object> requestBody = Map.of(
                    "type", "page",
                    "title", title,
                    "version", Map.of("number", newVersion),
                    "body", Map.of(
                            "storage", Map.of(
                                    "value", body,
                                    "representation", "storage"
                            )
                    )
            );

            String response = client.put()
                    .uri("/wiki/rest/api/content/{id}", id)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String pageId = root.path("id").asText();
            String spaceKey = root.path("space").path("key").asText();
            return properties.baseUrl() + "/wiki/spaces/" + spaceKey + "/pages/" + pageId;

        } catch (HttpClientErrorException e) {
            log.warn("Confluence updatePage error: id={}, status={}, body={}", id, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Confluence 페이지 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Confluence updatePage error: {}", e.getMessage());
            throw new ExternalServiceException("Confluence 페이지 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
