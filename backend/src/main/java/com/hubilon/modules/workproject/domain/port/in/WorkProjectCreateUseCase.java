package com.hubilon.modules.workproject.domain.port.in;

import com.hubilon.modules.workproject.application.dto.WorkProjectCreateCommand;
import com.hubilon.modules.workproject.application.dto.WorkProjectResult;

public interface WorkProjectCreateUseCase {
    WorkProjectResult create(WorkProjectCreateCommand command);
}
