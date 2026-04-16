package com.hubilon.modules.scheduler.application.service;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerJobLogQueryUseCase;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchedulerJobLogQueryService implements SchedulerJobLogQueryUseCase {

    private final SchedulerJobLogQueryPort schedulerJobLogQueryPort;

    @Override
    public Page<SchedulerJobLog> findAll(Pageable pageable) {
        return schedulerJobLogQueryPort.findAll(pageable);
    }

    @Override
    public Optional<SchedulerJobLog> findById(Long id) {
        return schedulerJobLogQueryPort.findById(id);
    }
}
