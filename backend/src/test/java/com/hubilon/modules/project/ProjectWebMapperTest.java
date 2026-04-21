package com.hubilon.modules.project;

import com.hubilon.modules.project.adapter.in.web.ProjectRegisterRequest;
import com.hubilon.modules.project.adapter.in.web.ProjectWebMapper;
import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.model.Project.AuthType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectWebMapperTest {

    ProjectWebMapper mapper = new ProjectWebMapper();

    @Test
    void gitProvider_null이면_GITLAB으로_변환() {
        ProjectRegisterRequest request = new ProjectRegisterRequest(
                "https://gitlab.com/owner/repo", null, "token", AuthType.PAT, null
        );

        ProjectRegisterCommand command = mapper.toCommand(request, 1L);

        assertThat(command.gitProvider()).isEqualTo(GitProvider.GITLAB);
    }

    @Test
    void gitProvider_GITHUB이면_GITHUB_유지() {
        ProjectRegisterRequest request = new ProjectRegisterRequest(
                "https://github.com/owner/repo", null, "token", AuthType.OAUTH, GitProvider.GITHUB
        );

        ProjectRegisterCommand command = mapper.toCommand(request, 1L);

        assertThat(command.gitProvider()).isEqualTo(GitProvider.GITHUB);
    }

    @Test
    void gitProvider_GITLAB이면_GITLAB_유지() {
        ProjectRegisterRequest request = new ProjectRegisterRequest(
                "https://gitlab.com/owner/repo", null, "token", AuthType.PAT, GitProvider.GITLAB
        );

        ProjectRegisterCommand command = mapper.toCommand(request, 1L);

        assertThat(command.gitProvider()).isEqualTo(GitProvider.GITLAB);
    }
}
