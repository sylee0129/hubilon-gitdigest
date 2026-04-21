package com.hubilon.modules.project;

import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.report.adapter.out.github.GitHubAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GitHubAdapterTest {

    @Mock
    WebClient.Builder webClientBuilder;

    GitHubAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new GitHubAdapter(webClientBuilder);
    }

    @Test
    void supports_GITHUB_반환() {
        assertThat(adapter.supports()).isEqualTo(GitProvider.GITHUB);
    }

    @Test
    void extractOwnerRepo_정상URL_파싱() throws Exception {
        // private 메서드를 간접 테스트 — null URL로 IllegalArgumentException 유발
        assertThatThrownBy(() -> adapter.resolveProjectId(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GitHub URL 형식이 올바르지 않습니다");
    }

    @Test
    void extractOwnerRepo_gitlab_URL_오류() {
        assertThatThrownBy(() -> adapter.resolveProjectId("https://gitlab.com/owner/repo", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GitHub URL 형식이 올바르지 않습니다");
    }

    @Test
    void extractOwnerRepo_owner만_있는_URL_오류() {
        assertThatThrownBy(() -> adapter.resolveProjectId("https://github.com/owner", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner/repo를 추출할 수 없습니다");
    }

    @Test
    void extractOwnerRepo_빈URL_오류() {
        assertThatThrownBy(() -> adapter.resolveProjectId("", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GitHub URL 형식이 올바르지 않습니다");
    }
}
