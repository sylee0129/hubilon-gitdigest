package com.hubilon.modules.scheduler.domain.port.out;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;

public interface SchedulerJobLogCommandPort {

    SchedulerJobLog save(SchedulerJobLog jobLog);
}
