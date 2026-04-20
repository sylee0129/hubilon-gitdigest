package com.hubilon.modules.department.domain.port.in;

import com.hubilon.modules.department.application.dto.DepartmentWithTeamsResult;

import java.util.List;

public interface DepartmentQueryUseCase {
    List<DepartmentWithTeamsResult> findAllWithTeams();
    DepartmentWithTeamsResult findWithTeamsById(Long deptId);
}
