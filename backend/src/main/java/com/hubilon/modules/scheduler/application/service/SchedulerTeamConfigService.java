package com.hubilon.modules.scheduler.application.service;

import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerTeamConfigUseCase;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerTeamConfigPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulerTeamConfigService implements SchedulerTeamConfigUseCase {

    private final SchedulerTeamConfigPort schedulerTeamConfigPort;
    private final TeamQueryPort teamQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<SchedulerTeamConfig> findAll() {
        Map<Long, SchedulerTeamConfig> configByTeamId = schedulerTeamConfigPort.findAll().stream()
                .collect(Collectors.toMap(SchedulerTeamConfig::teamId, c -> c));

        return teamQueryPort.findAll().stream()
                .map(team -> configByTeamId.getOrDefault(
                        team.getId(),
                        SchedulerTeamConfig.create(team.getId(), team.getName())
                ))
                .toList();
    }

    @Override
    @Transactional
    public SchedulerTeamConfig upsert(Long teamId, boolean enabled) {
        Team team = teamQueryPort.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다. teamId=" + teamId));

        SchedulerTeamConfig config = schedulerTeamConfigPort.findByTeamId(teamId)
                .map(existing -> existing.withEnabled(enabled))
                .orElseGet(() -> SchedulerTeamConfig.create(teamId, team.getName()).withEnabled(enabled));

        return schedulerTeamConfigPort.save(config);
    }
}
