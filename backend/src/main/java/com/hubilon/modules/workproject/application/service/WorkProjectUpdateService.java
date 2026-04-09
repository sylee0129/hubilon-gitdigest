package com.hubilon.modules.workproject.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.workproject.application.dto.WorkProjectResult;
import com.hubilon.modules.workproject.application.dto.WorkProjectUpdateCommand;
import com.hubilon.modules.workproject.domain.model.WorkProject;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectUpdateUseCase;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectCommandPort;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkProjectUpdateService implements WorkProjectUpdateUseCase {

    private final WorkProjectCommandPort workProjectCommandPort;
    private final WorkProjectQueryPort workProjectQueryPort;

    @Override
    public WorkProjectResult update(Long id, WorkProjectUpdateCommand command) {
        WorkProject existing = workProjectQueryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("세부 프로젝트를 찾을 수 없습니다. id=" + id));
        WorkProject wp = WorkProject.builder()
                .id(id)
                .folderId(command.folderId())
                .name(command.name())
                .sortOrder(existing.getSortOrder())
                .build();
        WorkProject saved = workProjectCommandPort.save(wp);
        return new WorkProjectResult(saved.getId(), saved.getFolderId(), saved.getName(), saved.getSortOrder());
    }
}
