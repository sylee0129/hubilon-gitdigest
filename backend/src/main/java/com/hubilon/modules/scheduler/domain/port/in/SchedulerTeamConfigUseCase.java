package com.hubilon.modules.scheduler.domain.port.in;

import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;

import java.util.List;

public interface SchedulerTeamConfigUseCase {

    List<SchedulerTeamConfig> findAll();

    SchedulerTeamConfig upsert(Long teamId, boolean enabled);
}
