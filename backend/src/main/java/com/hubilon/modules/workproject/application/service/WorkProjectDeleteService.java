package com.hubilon.modules.workproject.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectDeleteUseCase;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectCommandPort;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkProjectDeleteService implements WorkProjectDeleteUseCase {

    private final WorkProjectCommandPort workProjectCommandPort;
    private final WorkProjectQueryPort workProjectQueryPort;

    @Override
    public void delete(Long id) {
        if (!workProjectQueryPort.existsById(id)) {
            throw new NotFoundException("세부 프로젝트를 찾을 수 없습니다. id=" + id);
        }
        workProjectCommandPort.deleteById(id);
    }
}
