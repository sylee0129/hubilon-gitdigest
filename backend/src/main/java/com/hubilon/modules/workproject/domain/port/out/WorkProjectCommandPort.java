package com.hubilon.modules.workproject.domain.port.out;

import com.hubilon.modules.workproject.application.dto.WorkProjectReorderCommand;
import com.hubilon.modules.workproject.domain.model.WorkProject;

import java.util.List;

public interface WorkProjectCommandPort {
    WorkProject save(WorkProject workProject);
    void deleteById(Long id);
    void updateSortOrders(List<WorkProjectReorderCommand.OrderItem> orders);
}
