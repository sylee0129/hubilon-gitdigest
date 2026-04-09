package com.hubilon.modules.workproject.domain.port.in;

import com.hubilon.modules.workproject.application.dto.WorkProjectResult;
import com.hubilon.modules.workproject.application.dto.WorkProjectUpdateCommand;

public interface WorkProjectUpdateUseCase {
    WorkProjectResult update(Long id, WorkProjectUpdateCommand command);
}
