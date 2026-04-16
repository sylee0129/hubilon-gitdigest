package com.hubilon.modules.scheduler.adapter.out.persistence;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerJobLogRepository extends JpaRepository<SchedulerJobLogJpaEntity, Long> {

    boolean existsByStatus(SchedulerJobStatus status);
}
