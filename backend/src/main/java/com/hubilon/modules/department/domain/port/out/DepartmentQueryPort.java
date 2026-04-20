package com.hubilon.modules.department.domain.port.out;

import com.hubilon.modules.department.domain.model.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentQueryPort {
    List<Department> findAll();
    Optional<Department> findById(Long id);
}
