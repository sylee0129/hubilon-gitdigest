package com.hubilon.modules.project;

import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.application.dto.ProjectRegisterResult;
import com.hubilon.modules.project.application.mapper.ProjectAppMapper;
import com.hubilon.modules.project.application.service.command.ProjectRegisterService;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.model.Project.AuthType;
import com.hubilon.modules.project.domain.port.out.GitProviderAdapter;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectRegisterServiceStrategyTest {

    @Mock
    ProjectCommandPort projectCommandPort;

    @Mock
    ProjectAppMapper projectAppMapper;

    @Mock
    GitProviderAdapter gitLabAdapter;

    @Mock
    GitProviderAdapter gitHubAdapter;

    ProjectRegisterService service;

    @BeforeEach
    void setUp() {
        when(gitLabAdapter.supports()).thenReturn(GitProvider.GITLAB);
        when(gitHubAdapter.supports()).thenReturn(GitProvider.GITHUB);
        service = new ProjectRegisterService(List.of(gitLabAdapter, gitHubAdapter), projectCommandPort, projectAppMapper);
    }

    @Test
    void gitProvider_null이면_GITLAB_어댑터_사용() {
        ProjectRegisterCommand command = new ProjectRegisterCommand(
                "https://gitlab.com/owner/repo", null, "token", AuthType.PAT, 1L, null
        );

        Project savedProject = Project.builder()
                .id(1L).name("repo").gitlabUrl("https://gitlab.com/owner/repo")
                .gitProvider(GitProvider.GITLAB).build();

        when(gitLabAdapter.resolveProjectId(any(), any())).thenReturn(123L);
        when(gitLabAdapter.resolveProjectName(any(), any())).thenReturn("repo");
        when(projectCommandPort.save(any())).thenReturn(savedProject);
        when(projectAppMapper.toRegisterResult(any())).thenReturn(
                new ProjectRegisterResult(1L, "repo", "https://gitlab.com/owner/repo",
                        AuthType.PAT, 123L, LocalDateTime.now()));

        ProjectRegisterResult result = service.register(command);

        assertThat(result).isNotNull();
        verify(gitLabAdapter).resolveProjectId(any(), any());
        verify(gitHubAdapter, never()).resolveProjectId(any(), any());
    }

    @Test
    void gitProvider_GITHUB이면_GITHUB_어댑터_사용() {
        ProjectRegisterCommand command = new ProjectRegisterCommand(
                "https://github.com/owner/repo", null, "token", AuthType.OAUTH, 1L, GitProvider.GITHUB
        );

        Project savedProject = Project.builder()
                .id(2L).name("repo").gitlabUrl("https://github.com/owner/repo")
                .gitProvider(GitProvider.GITHUB).build();

        when(gitHubAdapter.resolveProjectId(any(), any())).thenReturn(456L);
        when(gitHubAdapter.resolveProjectName(any(), any())).thenReturn("repo");
        when(projectCommandPort.save(any())).thenReturn(savedProject);
        when(projectAppMapper.toRegisterResult(any())).thenReturn(
                new ProjectRegisterResult(2L, "repo", "https://github.com/owner/repo",
                        AuthType.OAUTH, 456L, LocalDateTime.now()));

        ProjectRegisterResult result = service.register(command);

        assertThat(result).isNotNull();
        verify(gitHubAdapter).resolveProjectId(any(), any());
        verify(gitLabAdapter, never()).resolveProjectId(any(), any());
    }

    @Test
    void projectId_직접입력시_어댑터_호출_안함() {
        ProjectRegisterCommand command = new ProjectRegisterCommand(
                "https://github.com/owner/repo", 789L, "token", AuthType.PAT, 1L, GitProvider.GITHUB
        );

        Project savedProject = Project.builder()
                .id(3L).name("repo").gitlabUrl("https://github.com/owner/repo")
                .gitProvider(GitProvider.GITHUB).build();

        when(projectCommandPort.save(any())).thenReturn(savedProject);
        when(projectAppMapper.toRegisterResult(any())).thenReturn(
                new ProjectRegisterResult(3L, "repo", "https://github.com/owner/repo",
                        AuthType.PAT, 789L, LocalDateTime.now()));

        ProjectRegisterResult result = service.register(command);

        assertThat(result).isNotNull();
        verify(gitHubAdapter, never()).resolveProjectId(any(), any());
        verify(gitLabAdapter, never()).resolveProjectId(any(), any());
    }

    @Test
    void 지원하지않는_gitProvider이면_IllegalArgumentException() {
        // 어댑터가 없는 provider를 직접 adapterMap에서 찾을 방법은 없으므로
        // enum에 없는 값은 컴파일 단계에서 방어됨 — 빈 어댑터 목록으로 서비스 생성
        ProjectRegisterService emptyService = new ProjectRegisterService(
                List.of(), projectCommandPort, projectAppMapper
        );

        ProjectRegisterCommand command = new ProjectRegisterCommand(
                "https://github.com/owner/repo", null, "token", AuthType.PAT, 1L, GitProvider.GITHUB
        );

        assertThatThrownBy(() -> emptyService.register(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 Git 제공자");
    }
}
