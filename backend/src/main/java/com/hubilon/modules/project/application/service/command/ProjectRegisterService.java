package com.hubilon.modules.project.application.service.command;

import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.application.dto.ProjectRegisterResult;
import com.hubilon.modules.project.application.mapper.ProjectAppMapper;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.in.ProjectRegisterUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.report.adapter.out.gitlab.GitLabAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectRegisterService implements ProjectRegisterUseCase {

    private final ProjectCommandPort projectCommandPort;
    private final ProjectAppMapper projectAppMapper;
    private final GitLabAdapter gitLabAdapter;

    @Transactional
    @Override
    public ProjectRegisterResult register(ProjectRegisterCommand command) {
        log.info("Registering GitLab project: {}", command.gitlabUrl());

        String projectPath = extractProjectPath(command.gitlabUrl());
        Long gitlabProjectId;
        String projectName;

        if (command.gitlabProjectId() != null) {
            // Project ID 직접 입력 — read_api 없이 read_repository 스코프로 동작
            gitlabProjectId = command.gitlabProjectId();
            projectName = extractProjectName(command.gitlabUrl());
            log.info("Using provided gitlabProjectId={}, name={}", gitlabProjectId, projectName);
        } else {
            // URL로 프로젝트 조회 — read_api 스코프 필요
            gitlabProjectId = gitLabAdapter.resolveProjectId(projectPath, command.accessToken(), command.authType().name());
            projectName = gitLabAdapter.resolveProjectName(projectPath, command.accessToken(), command.authType().name());
        }

        Project project = Project.builder()
                .name(projectName)
                .gitlabUrl(command.gitlabUrl())
                .accessToken(command.accessToken())
                .authType(command.authType())
                .gitlabProjectId(gitlabProjectId)
                .build();

        Project saved = projectCommandPort.save(project);
        return projectAppMapper.toRegisterResult(saved);
    }

    private String extractProjectName(String gitlabUrl) {
        // https://gitlab.com/group/sub/project.git → "project"
        String path = gitlabUrl.replaceAll("\\.git$", "");
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String extractProjectPath(String gitlabUrl) {
        // https://gitlab.com/group/project.git → group/project
        String[] parts = gitlabUrl.split("/", -1);
        int hostParts = 3; // https:, "", gitlab.com
        if (parts.length <= hostParts) {
            return gitlabUrl;
        }
        StringBuilder path = new StringBuilder();
        for (int i = hostParts; i < parts.length; i++) {
            if (i > hostParts) path.append("/");
            path.append(parts[i]);
        }
        String result = path.toString();
        if (result.endsWith(".git")) {
            result = result.substring(0, result.length() - 4);
        }
        return result;
    }
}
