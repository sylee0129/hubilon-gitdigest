package com.hubilon.modules.project.domain.port.out;

import com.hubilon.modules.project.domain.model.Project;

import java.util.List;

public interface ProjectCommandPort {

    Project save(Project project);

    void deleteById(Long id);

    void updateSortOrders(List<Long> orderedIds);
}
