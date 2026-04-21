package com.hubilon.modules.project.application.service.command;

import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.application.dto.ProjectRegisterResult;
import com.hubilon.modules.project.application.mapper.ProjectAppMapper;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.in.ProjectRegisterUseCase;
import com.hubilon.modules.project.domain.port.out.GitProviderAdapter;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectRegisterService implements ProjectRegisterUseCase {

    private final ProjectCommandPort projectCommandPort;
    private final ProjectAppMapper projectAppMapper;
    private final Map<GitProvider, GitProviderAdapter> adapterMap;

    public ProjectRegisterService(List<GitProviderAdapter> adapters,
                                   ProjectCommandPort projectCommandPort,
                                   ProjectAppMapper projectAppMapper) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(GitProviderAdapter::supports, a -> a));
        this.projectCommandPort = projectCommandPort;
        this.projectAppMapper = projectAppMapper;
    }

    @Transactional
    @Override
    public ProjectRegisterResult register(ProjectRegisterCommand command) {
        GitProvider gitProvider = command.gitProvider() != null ? command.gitProvider() : GitProvider.GITLAB;
        log.info("Registering {} project: {}", gitProvider, command.gitlabUrl());

        Long resolvedProjectId;
        String projectName;

        if (command.gitlabProjectId() != null) {
            // Project ID 직접 입력 — read_api 없이 read_repository 스코프로 동작
            resolvedProjectId = command.gitlabProjectId();
            projectName = extractProjectName(command.gitlabUrl());
            log.info("Using provided projectId={}, name={}", resolvedProjectId, projectName);
        } else {
            GitProviderAdapter adapter = adapterMap.get(gitProvider);
            if (adapter == null) {
                throw new IllegalArgumentException("지원하지 않는 Git 제공자입니다: " + gitProvider);
            }
            resolvedProjectId = adapter.resolveProjectId(command.gitlabUrl(), command.accessToken());
            projectName = adapter.resolveProjectName(command.gitlabUrl(), command.accessToken());
        }

        Project project = Project.builder()
                .name(projectName)
                .gitlabUrl(command.gitlabUrl())
                .accessToken(command.accessToken())
                .authType(command.authType())
                .gitlabProjectId(resolvedProjectId)
                .teamId(command.teamId())
                .gitProvider(gitProvider)
                .build();

        Project saved = projectCommandPort.save(project);
        return projectAppMapper.toRegisterResult(saved);
    }

    private String extractProjectName(String repoUrl) {
        String path = repoUrl.replaceAll("\\.git$", "");
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
