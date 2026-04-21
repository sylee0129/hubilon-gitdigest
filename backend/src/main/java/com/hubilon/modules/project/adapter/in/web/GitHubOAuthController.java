package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.common.config.GitHubOAuthProperties;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/oauth/github")
@RequiredArgsConstructor
@Tag(name = "GitHub OAuth", description = "GitHub OAuth 인증 API")
public class GitHubOAuthController {

    private final GitHubOAuthProperties oauthProperties;
    private final WebClient.Builder webClientBuilder;

    // state 임시 저장 (GitLabOAuthController와 동일한 방식)
    private final ConcurrentHashMap<String, Boolean> stateStore = new ConcurrentHashMap<>();

    @GetMapping("/authorize")
    @Operation(summary = "GitHub OAuth 인증 URL 생성")
    public Response<Map<String, String>> getAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, Boolean.TRUE);

        String authUrl = "https://github.com/login/oauth/authorize"
                + "?client_id=" + oauthProperties.clientId()
                + "&redirect_uri=" + oauthProperties.redirectUri()
                + "&scope=repo"
                + "&state=" + state;

        return Response.ok(Map.of("authUrl", authUrl, "state", state));
    }

    @GetMapping("/callback")
    @Operation(summary = "GitHub OAuth 콜백 — 토큰 교환 후 팝업 종료")
    public ResponseEntity<String> callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        Boolean validState = stateStore.remove(state);
        if (validState == null) {
            log.warn("GitHub OAuth state mismatch or expired");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("invalid_state"));
        }

        try {
            String token = exchangeCodeForToken(code);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildSuccessHtml(token));
        } catch (Exception e) {
            log.warn("GitHub OAuth token exchange failed");
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorHtml("token_exchange_failed"));
        }
    }

    private String exchangeCodeForToken(String code) {
        record TokenResponse(String access_token, String error) {}

        TokenResponse response = webClientBuilder.build()
                .post()
                .uri("https://github.com/login/oauth/access_token")
                .headers(headers -> headers.set("Accept", "application/json"))
                .bodyValue(Map.of(
                        "client_id", oauthProperties.clientId(),
                        "client_secret", oauthProperties.clientSecret(),
                        "code", code
                ))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response == null || response.access_token() == null || response.access_token().isBlank()) {
            throw new IllegalStateException("GitHub으로부터 토큰을 받지 못했습니다.");
        }
        return response.access_token();
    }

    private String buildSuccessHtml(String token) {
        String origin = oauthProperties.frontendOrigin();
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><title>GitHub 인증 완료</title></head>
                <body>
                  <p>인증이 완료되었습니다. 잠시 후 창이 닫힙니다...</p>
                  <script>
                    try {
                      window.opener.postMessage(
                        { type: 'github-oauth', token: '%s', repoUrl: null },
                        '%s'
                      );
                    } catch(e) {}
                    window.close();
                  </script>
                </body>
                </html>
                """.formatted(token, origin);
    }

    private String buildErrorHtml(String errorCode) {
        String origin = oauthProperties.frontendOrigin();
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"><title>인증 실패</title></head>
                <body>
                  <p style="color:red;">인증에 실패했습니다.</p>
                  <script>
                    try {
                      window.opener.postMessage(
                        { type: 'github-oauth-error', message: '%s' },
                        '%s'
                      );
                    } catch(e) {}
                    window.close();
                  </script>
                </body>
                </html>
                """.formatted(errorCode, origin);
    }
}
