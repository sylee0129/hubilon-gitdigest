package com.hubilon.modules.scheduler.domain.port.out;

import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;

import java.util.List;
import java.util.Optional;

public interface SchedulerTeamConfigPort {

    List<SchedulerTeamConfig> findAll();

    List<SchedulerTeamConfig> findAllByEnabled(boolean enabled);

    Optional<SchedulerTeamConfig> findByTeamId(Long teamId);

    SchedulerTeamConfig save(SchedulerTeamConfig config);
}
