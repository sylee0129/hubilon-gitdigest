package com.hubilon.modules.scheduler.domain.port.in;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SchedulerJobLogQueryUseCase {

    Page<SchedulerJobLog> findAll(Pageable pageable);

    Optional<SchedulerJobLog> findById(Long id);
}
