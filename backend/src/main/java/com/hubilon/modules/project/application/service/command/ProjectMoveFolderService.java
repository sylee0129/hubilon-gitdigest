package com.hubilon.modules.project.application.service.command;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.in.ProjectMoveFolderUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMoveFolderService implements ProjectMoveFolderUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ProjectCommandPort projectCommandPort;

    @Transactional
    @Override
    public void moveToFolder(Long projectId, Long folderId) {
        Project existing = projectQueryPort.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + projectId));
        Project updated = Project.builder()
                .id(existing.getId())
                .name(existing.getName())
                .gitlabUrl(existing.getGitlabUrl())
                .accessToken(existing.getAccessToken())
                .authType(existing.getAuthType())
                .gitlabProjectId(existing.getGitlabProjectId())
                .sortOrder(existing.getSortOrder())
                .folderId(folderId)
                .teamId(existing.getTeamId())
                .build();
        projectCommandPort.save(updated);
    }
}
