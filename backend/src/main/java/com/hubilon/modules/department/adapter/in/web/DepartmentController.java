package com.hubilon.modules.department.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.department.application.dto.DepartmentWithTeamsResult;
import com.hubilon.modules.department.domain.port.in.DepartmentQueryUseCase;
import com.hubilon.modules.team.adapter.in.web.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Departments", description = "부서 관리 API")
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentQueryUseCase departmentQueryUseCase;

    @Operation(summary = "부서 전체 목록 조회 (팀 포함)")
    @GetMapping
    public Response<List<DepartmentResponse>> findAll() {
        List<DepartmentResponse> responses = departmentQueryUseCase.findAllWithTeams().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(responses);
    }

    @Operation(summary = "부서 단건 조회 (팀 포함)")
    @GetMapping("/{deptId}")
    public Response<DepartmentResponse> findById(@PathVariable Long deptId) {
        DepartmentWithTeamsResult result = departmentQueryUseCase.findWithTeamsById(deptId);
        return Response.ok(toResponse(result));
    }

    private DepartmentResponse toResponse(DepartmentWithTeamsResult result) {
        List<TeamResponse> teams = result.teams().stream()
                .map(t -> new TeamResponse(t.id(), t.name()))
                .toList();
        return new DepartmentResponse(result.id(), result.name(), teams);
    }
}
