package com.hubilon.modules.scheduler.adapter.out.persistence;

import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerTeamConfigPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SchedulerTeamConfigPersistenceAdapter implements SchedulerTeamConfigPort {

    private final SchedulerTeamConfigRepository schedulerTeamConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SchedulerTeamConfig> findAll() {
        return schedulerTeamConfigRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulerTeamConfig> findAllByEnabled(boolean enabled) {
        return schedulerTeamConfigRepository.findAllByEnabled(enabled).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SchedulerTeamConfig> findByTeamId(Long teamId) {
        return schedulerTeamConfigRepository.findByTeamId(teamId).map(this::toDomain);
    }

    @Override
    @Transactional
    public SchedulerTeamConfig save(SchedulerTeamConfig config) {
        SchedulerTeamConfigJpaEntity entity = schedulerTeamConfigRepository
                .findByTeamId(config.teamId())
                .orElseGet(() -> SchedulerTeamConfigJpaEntity.builder()
                        .teamId(config.teamId())
                        .teamName(config.teamName())
                        .enabled(config.enabled())
                        .build());

        entity.updateEnabled(config.enabled());
        return toDomain(schedulerTeamConfigRepository.save(entity));
    }

    private SchedulerTeamConfig toDomain(SchedulerTeamConfigJpaEntity entity) {
        return new SchedulerTeamConfig(entity.getId(), entity.getTeamId(), entity.getTeamName(), entity.isEnabled());
    }
}
