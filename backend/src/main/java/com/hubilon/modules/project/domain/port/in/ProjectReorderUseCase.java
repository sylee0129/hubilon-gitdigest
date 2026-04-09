package com.hubilon.modules.project.domain.port.in;

import com.hubilon.modules.project.application.dto.ProjectReorderCommand;

public interface ProjectReorderUseCase {

    void reorder(ProjectReorderCommand command);
}
