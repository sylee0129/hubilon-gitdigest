package com.hubilon.modules.scheduler.domain.port.out;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SchedulerJobLogQueryPort {

    Page<SchedulerJobLog> findAll(Pageable pageable);

    Optional<SchedulerJobLog> findById(Long id);

    boolean existsByStatus(SchedulerJobStatus status);
}
