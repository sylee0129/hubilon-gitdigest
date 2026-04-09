package com.hubilon.modules.workproject.domain.port.in;

import com.hubilon.modules.workproject.application.dto.WorkProjectReorderCommand;

public interface WorkProjectReorderUseCase {
    void reorder(WorkProjectReorderCommand command);
}
