package com.hubilon.modules.scheduler.domain.model;

public record SchedulerTeamConfig(Long id, Long teamId, String teamName, boolean enabled) {

    public static SchedulerTeamConfig create(Long teamId, String teamName) {
        return new SchedulerTeamConfig(null, teamId, teamName, true);
    }

    public SchedulerTeamConfig withEnabled(boolean enabled) {
        return new SchedulerTeamConfig(id, teamId, teamName, enabled);
    }
}
