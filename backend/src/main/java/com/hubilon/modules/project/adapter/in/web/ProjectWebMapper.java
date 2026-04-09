package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.application.dto.ProjectRegisterResult;
import com.hubilon.modules.project.application.dto.ProjectSearchResult;
import org.springframework.stereotype.Component;

@Component
public class ProjectWebMapper {

    public ProjectRegisterCommand toCommand(ProjectRegisterRequest request) {
        return new ProjectRegisterCommand(
                request.gitlabUrl(),
                request.gitlabProjectId(),
                request.accessToken(),
                request.authType()
        );
    }

    public ProjectRegisterResponse toResponse(ProjectRegisterResult result) {
        return new ProjectRegisterResponse(
                result.id(),
                result.name(),
                result.gitlabUrl(),
                result.authType(),
                result.gitlabProjectId(),
                result.createdAt()
        );
    }

    public ProjectSearchResponse toSearchResponse(ProjectSearchResult result) {
        return new ProjectSearchResponse(
                result.id(),
                result.name(),
                result.gitlabUrl(),
                result.authType(),
                result.gitlabProjectId(),
                result.sortOrder(),
                result.createdAt()
        );
    }
}
