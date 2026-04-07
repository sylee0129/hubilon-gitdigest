package com.hubilon.modules.project.application.service.command;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.domain.port.in.ProjectDeleteUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDeleteService implements ProjectDeleteUseCase {

    private final ProjectCommandPort projectCommandPort;
    private final ProjectQueryPort projectQueryPort;

    @Transactional
    @Override
    public void delete(Long id) {
        log.info("Deleting project id={}", id);
        if (!projectQueryPort.existsById(id)) {
            throw new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id);
        }
        projectCommandPort.deleteById(id);
    }
}
