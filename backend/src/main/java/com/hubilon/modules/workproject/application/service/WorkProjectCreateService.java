package com.hubilon.modules.workproject.application.service;

import com.hubilon.modules.workproject.application.dto.WorkProjectCreateCommand;
import com.hubilon.modules.workproject.application.dto.WorkProjectResult;
import com.hubilon.modules.workproject.domain.model.WorkProject;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectCreateUseCase;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkProjectCreateService implements WorkProjectCreateUseCase {

    private final WorkProjectCommandPort workProjectCommandPort;

    @Override
    public WorkProjectResult create(WorkProjectCreateCommand command) {
        WorkProject wp = WorkProject.builder()
                .folderId(command.folderId())
                .name(command.name())
                .sortOrder(0)
                .build();
        WorkProject saved = workProjectCommandPort.save(wp);
        return new WorkProjectResult(saved.getId(), saved.getFolderId(), saved.getName(), saved.getSortOrder());
    }
}
