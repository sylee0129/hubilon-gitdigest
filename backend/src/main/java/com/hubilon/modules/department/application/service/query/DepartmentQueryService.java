package com.hubilon.modules.department.application.service.query;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.department.application.dto.DepartmentWithTeamsResult;
import com.hubilon.modules.department.application.dto.TeamResult;
import com.hubilon.modules.department.domain.model.Department;
import com.hubilon.modules.department.domain.port.in.DepartmentQueryUseCase;
import com.hubilon.modules.department.domain.port.out.DepartmentQueryPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentQueryService implements DepartmentQueryUseCase {

    private final DepartmentQueryPort departmentQueryPort;
    private final TeamQueryPort teamQueryPort;

    @Override
    public List<DepartmentWithTeamsResult> findAllWithTeams() {
        return departmentQueryPort.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public DepartmentWithTeamsResult findWithTeamsById(Long deptId) {
        Department department = departmentQueryPort.findById(deptId)
                .orElseThrow(() -> new NotFoundException("부서를 찾을 수 없습니다. id=" + deptId));
        return toResult(department);
    }

    private DepartmentWithTeamsResult toResult(Department department) {
        List<TeamResult> teams = teamQueryPort.findByDeptId(department.getId()).stream()
                .map(team -> new TeamResult(team.getId(), team.getName()))
                .toList();
        return new DepartmentWithTeamsResult(department.getId(), department.getName(), teams);
    }
}
