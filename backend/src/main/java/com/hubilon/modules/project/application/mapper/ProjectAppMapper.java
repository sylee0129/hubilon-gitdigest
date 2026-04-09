package com.hubilon.modules.project.application.mapper;

import com.hubilon.modules.project.application.dto.ProjectRegisterResult;
import com.hubilon.modules.project.application.dto.ProjectSearchResult;
import com.hubilon.modules.project.domain.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectAppMapper {

    public ProjectRegisterResult toRegisterResult(Project project) {
        return new ProjectRegisterResult(
                project.getId(),
                project.getName(),
                project.getGitlabUrl(),
                project.getAuthType(),
                project.getGitlabProjectId(),
                project.getCreatedAt()
        );
    }

    public ProjectSearchResult toSearchResult(Project project) {
        return new ProjectSearchResult(
                project.getId(),
                project.getName(),
                project.getGitlabUrl(),
                project.getAuthType(),
                project.getGitlabProjectId(),
                project.getSortOrder(),
                project.getCreatedAt()
        );
    }
}
