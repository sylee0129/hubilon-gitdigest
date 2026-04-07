package com.hubilon.modules.project.domain.port.out;

import com.hubilon.modules.project.domain.model.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectQueryPort {

    List<Project> findAll();

    Optional<Project> findById(Long id);

    boolean existsById(Long id);
}
