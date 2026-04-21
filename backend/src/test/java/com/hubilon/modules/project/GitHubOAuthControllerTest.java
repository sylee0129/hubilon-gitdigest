package com.hubilon.modules.project;

import com.hubilon.common.config.GitHubOAuthProperties;
import com.hubilon.modules.project.adapter.in.web.GitHubOAuthController;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.common.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubOAuthControllerTest {

    @Mock
    GitHubOAuthProperties oauthProperties;

    @Mock
    WebClient.Builder webClientBuilder;

    GitHubOAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new GitHubOAuthController(oauthProperties, webClientBuilder);
    }

    @Test
    void authorize_state_생성_및_authUrl_반환() {
        when(oauthProperties.clientId()).thenReturn("test-client-id");
        when(oauthProperties.redirectUri()).thenReturn("http://localhost:8080/callback");

        Response<Map<String, String>> response = controller.getAuthorizeUrl();

        assertThat(response).isNotNull();
        Map<String, String> data = response.data();
        assertThat(data).containsKey("authUrl");
        assertThat(data).containsKey("state");
        assertThat(data.get("authUrl")).contains("client_id=test-client-id");
        assertThat(data.get("authUrl")).contains("scope=repo");
        assertThat(data.get("state")).isNotBlank();
    }

    @Test
    void callback_state_불일치시_오류_HTML_반환() {
        when(oauthProperties.frontendOrigin()).thenReturn("http://localhost:3000");

        ResponseEntity<String> response = controller.callback("some-code", "invalid-state");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("github-oauth-error");
        assertThat(response.getBody()).contains("invalid_state");
    }

    @Test
    void callback_state_검증_후_재사용_불가() {
        when(oauthProperties.clientId()).thenReturn("test-client-id");
        when(oauthProperties.redirectUri()).thenReturn("http://localhost:8080/callback");

        // authorize 호출로 state 생성
        Response<Map<String, String>> authorizeResponse = controller.getAuthorizeUrl();
        String validState = authorizeResponse.data().get("state");

        // 동일 state 두 번 사용 시 두 번째는 실패해야 함
        when(oauthProperties.frontendOrigin()).thenReturn("http://localhost:3000");

        // 첫 번째 callback — state가 존재하지만 토큰 교환은 실패할 것
        // (WebClient mock 미설정 → NullPointerException → error HTML 반환)
        ResponseEntity<String> firstResponse = controller.callback("some-code", validState);
        // 토큰 교환 실패지만 state는 소비됨 (remove됨)

        // 두 번째 callback — 이미 소비된 state
        ResponseEntity<String> secondResponse = controller.callback("some-code", validState);

        assertThat(secondResponse.getBody()).contains("invalid_state");
    }
}
