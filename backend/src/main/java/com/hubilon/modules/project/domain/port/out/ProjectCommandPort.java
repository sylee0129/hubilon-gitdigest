package com.hubilon.modules.project.domain.port.out;

import com.hubilon.modules.project.domain.model.Project;

public interface ProjectCommandPort {

    Project save(Project project);

    void deleteById(Long id);
}
