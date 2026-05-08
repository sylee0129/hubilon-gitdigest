package com.hubilon.modules.department.domain.port.out;

import com.hubilon.modules.department.domain.model.Department;

public interface DepartmentCommandPort {
    Department save(Department department);
}
