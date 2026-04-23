package com.hubilon.modules.scheduler.domain.port.in;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;

public interface SchedulerTriggerUseCase {

    SchedulerJobLog trigger(Long teamId);
}
