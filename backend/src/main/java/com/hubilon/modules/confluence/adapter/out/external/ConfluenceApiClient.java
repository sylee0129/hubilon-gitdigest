package com.hubilon.modules.confluence.adapter.out.external;

import com.hubilon.common.exception.custom.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ConfluenceApiClient {

    public record ConfluencePage(String id, int version, String url) {}

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final RestClient restClient;

    public ConfluenceApiClient(String baseUrl, String userEmail, String plainApiToken) {
        this.baseUrl = baseUrl;
        String credentials = userEmail + ":" + plainApiToken;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Optional<ConfluencePage> findPageByTitle(String spaceKey, String title) {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/wiki/rest/api/content")
                            .queryParam("spaceKey", spaceKey)
                            .queryParam("title", title)
                            .queryParam("expand", "version")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode results = root.path("results");

            if (results.isEmpty()) {
                return Optional.empty();
            }

            JsonNode page = results.get(0);
            String id = page.path("id").asText();
            int version = page.path("version").path("number").asInt();
            String url = baseUrl + "/wiki/spaces/" + spaceKey + "/pages/" + id;

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

            String response = restClient.post()
                    .uri("/wiki/rest/api/content")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = OBJECT_MAPPER.readTree(response);
            String id = root.path("id").asText();
            return baseUrl + "/wiki/spaces/" + spaceKey + "/pages/" + id;

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

            String response = restClient.put()
                    .uri("/wiki/rest/api/content/{id}", id)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = OBJECT_MAPPER.readTree(response);
            String pageId = root.path("id").asText();
            String spaceKey = root.path("space").path("key").asText();
            return baseUrl + "/wiki/spaces/" + spaceKey + "/pages/" + pageId;

        } catch (HttpClientErrorException e) {
            log.warn("Confluence updatePage error: id={}, status={}, body={}", id, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceException("Confluence 페이지 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Confluence updatePage error: {}", e.getMessage());
            throw new ExternalServiceException("Confluence 페이지 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 연결 테스트: GET /wiki/rest/api/space/{spaceKey}
     */
    public void testConnection(String spaceKey) {
        try {
            restClient.get()
                    .uri("/wiki/rest/api/space/{spaceKey}", spaceKey)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            log.warn("Confluence testConnection error: spaceKey={}, status={}", spaceKey, e.getStatusCode());
            throw new ExternalServiceException("Confluence 연결 테스트 실패: " + e.getStatusCode() + " " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Confluence testConnection error: {}", e.getMessage());
            throw new ExternalServiceException("Confluence 연결 테스트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
