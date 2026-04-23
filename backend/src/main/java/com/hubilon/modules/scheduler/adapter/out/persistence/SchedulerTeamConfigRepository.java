package com.hubilon.modules.scheduler.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulerTeamConfigRepository extends JpaRepository<SchedulerTeamConfigJpaEntity, Long> {

    Optional<SchedulerTeamConfigJpaEntity> findByTeamId(Long teamId);

    List<SchedulerTeamConfigJpaEntity> findAllByEnabled(boolean enabled);
}
