package com.hubilon.modules.workproject.domain.port.out;

import com.hubilon.modules.workproject.domain.model.WorkProject;

import java.util.List;
import java.util.Optional;

public interface WorkProjectQueryPort {
    Optional<WorkProject> findById(Long id);
    boolean existsById(Long id);
    List<WorkProject> findByFolderId(Long folderId);
}
