package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.common.config.GitLabOAuthProperties;
import com.hubilon.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/oauth/gitlab")
@RequiredArgsConstructor
@Tag(name = "GitLab OAuth", description = "GitLab OAuth 인증 API")
public class GitLabOAuthController {

    private final GitLabOAuthProperties oauthProperties;
    private final WebClient.Builder webClientBuilder;

    // state → gitlabUrl 임시 저장 (TTL 없음, 개발용)
    private final ConcurrentHashMap<String, String> stateStore = new ConcurrentHashMap<>();

    @GetMapping("/authorize")
    @Operation(summary = "GitLab OAuth 인증 URL 생성")
    public Response<Map<String, String>> getAuthorizeUrl(
            @RequestParam String gitlabUrl
    ) {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, gitlabUrl);

        String gitlabBase = extractGitlabBase(gitlabUrl);
        String authUrl = gitlabBase + "/oauth/authorize"
                + "?client_id=" + oauthProperties.clientId()
                + "&redirect_uri=" + oauthProperties.redirectUri()
                + "&response_type=code"
                + "&state=" + state
                + "&scope=read_api+api";

        return Response.ok(Map.of("authUrl", authUrl, "state", state));
    }

    @GetMapping("/callback")
    @Operation(summary = "GitLab OAuth 콜백 — 토큰 교환 후 팝업 종료")
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        String gitlabUrl = stateStore.remove(state);
        if (gitlabUrl == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("유효하지 않은 state 파라미터입니다."));
        }

        try {
            String gitlabBase = extractGitlabBase(gitlabUrl);
            String token = exchangeCodeForToken(gitlabBase, code);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildSuccessHtml(token, gitlabUrl));

        } catch (Exception e) {
            log.warn("OAuth token exchange failed: {}", e.getMessage());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("토큰 교환에 실패했습니다: " + e.getMessage()));
        }
    }

    private String exchangeCodeForToken(String gitlabBase, String code) {
        record TokenResponse(String access_token) {}

        TokenResponse response = webClientBuilder.build()
                .post()
                .uri(URI.create(gitlabBase + "/oauth/token"))
                .bodyValue(Map.of(
                        "client_id", oauthProperties.clientId(),
                        "client_secret", oauthProperties.clientSecret(),
                        "code", code,
                        "grant_type", "authorization_code",
                        "redirect_uri", oauthProperties.redirectUri()
                ))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response == null || response.access_token() == null) {
            throw new IllegalStateException("GitLab으로부터 토큰을 받지 못했습니다.");
        }
        return response.access_token();
    }

    private String extractGitlabBase(String gitlabUrl) {
        // https://gitlab.com/group/repo → https://gitlab.com
        try {
            URI uri = URI.create(gitlabUrl);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return "https://gitlab.com";
        }
    }

    private String buildSuccessHtml(String token, String gitlabUrl) {
        String origin = oauthProperties.frontendOrigin();
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><title>GitLab 인증 완료</title></head>
                <body>
                  <p>인증이 완료되었습니다. 잠시 후 창이 닫힙니다...</p>
                  <script>
                    try {
                      window.opener.postMessage(
                        { type: 'gitlab-oauth', token: '%s', gitlabUrl: '%s' },
                        '%s'
                      );
                    } catch(e) {}
                    window.close();
                  </script>
                </body>
                </html>
                """.formatted(token, gitlabUrl, origin);
    }

    private String buildErrorHtml(String message) {
        String origin = oauthProperties.frontendOrigin();
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><title>인증 실패</title></head>
                <body>
                  <p style="color:red;">%s</p>
                  <script>
                    try {
                      window.opener.postMessage(
                        { type: 'gitlab-oauth-error', message: '%s' },
                        '%s'
                      );
                    } catch(e) {}
                    window.close();
                  </script>
                </body>
                </html>
                """.formatted(message, message, origin);
    }
}
