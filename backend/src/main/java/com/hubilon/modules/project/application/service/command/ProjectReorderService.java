package com.hubilon.modules.project.application.service.command;

import com.hubilon.modules.project.application.dto.ProjectReorderCommand;
import com.hubilon.modules.project.domain.port.in.ProjectReorderUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectReorderService implements ProjectReorderUseCase {

    private final ProjectCommandPort projectCommandPort;

    @Transactional
    @Override
    public void reorder(ProjectReorderCommand command) {
        projectCommandPort.updateSortOrders(command.projectIds());
    }
}
