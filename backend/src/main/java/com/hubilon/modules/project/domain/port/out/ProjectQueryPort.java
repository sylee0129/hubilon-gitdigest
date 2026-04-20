package com.hubilon.modules.project.domain.port.out;

import com.hubilon.modules.project.domain.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectQueryPort {

    List<Project> findAll(Long teamId);

    Optional<Project> findById(Long id);

    List<Project> findByFolderId(Long folderId);

    boolean existsById(Long id);
}
