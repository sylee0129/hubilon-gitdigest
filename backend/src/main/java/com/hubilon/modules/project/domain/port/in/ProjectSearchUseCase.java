package com.hubilon.modules.project.domain.port.in;

import com.hubilon.modules.project.application.dto.ProjectSearchResult;

import java.util.List;

public interface ProjectSearchUseCase {

    List<ProjectSearchResult> searchAll(Long teamId);
}
