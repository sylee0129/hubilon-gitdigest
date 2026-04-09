package com.hubilon.modules.workproject.application.service;

import com.hubilon.modules.workproject.application.dto.WorkProjectReorderCommand;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectReorderUseCase;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkProjectReorderService implements WorkProjectReorderUseCase {

    private final WorkProjectCommandPort workProjectCommandPort;

    @Override
    public void reorder(WorkProjectReorderCommand command) {
        workProjectCommandPort.updateSortOrders(command.orders());
    }
}
