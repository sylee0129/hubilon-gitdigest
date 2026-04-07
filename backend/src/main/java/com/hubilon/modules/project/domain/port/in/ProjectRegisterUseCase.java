package com.hubilon.modules.project.domain.port.in;

import com.hubilon.modules.project.application.dto.ProjectRegisterCommand;
import com.hubilon.modules.project.application.dto.ProjectRegisterResult;

public interface ProjectRegisterUseCase {

    ProjectRegisterResult register(ProjectRegisterCommand command);
}
