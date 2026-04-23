package com.hubilon.modules.scheduler.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerTeamConfigUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scheduler Team Config", description = "팀별 스케줄러 설정 관리")
@RestController
@RequestMapping("/api/scheduler/team-configs")
@RequiredArgsConstructor
public class SchedulerTeamConfigController {

    private final SchedulerTeamConfigUseCase schedulerTeamConfigUseCase;

    @Operation(summary = "팀 스케줄러 설정 전체 조회")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<SchedulerTeamConfigResponse>> findAll() {
        List<SchedulerTeamConfigResponse> result = schedulerTeamConfigUseCase.findAll().stream()
                .map(SchedulerTeamConfigController::toResponse)
                .toList();
        return Response.ok(result);
    }

    @Operation(summary = "팀 스케줄러 활성 여부 설정")
    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<SchedulerTeamConfigResponse> upsert(
            @PathVariable Long teamId,
            @RequestBody UpdateTeamConfigRequest request
    ) {
        SchedulerTeamConfig config = schedulerTeamConfigUseCase.upsert(teamId, request.enabled());
        return Response.ok(toResponse(config));
    }

    private static SchedulerTeamConfigResponse toResponse(SchedulerTeamConfig config) {
        return new SchedulerTeamConfigResponse(config.id(), config.teamId(), config.teamName(), config.enabled());
    }

    public record UpdateTeamConfigRequest(boolean enabled) {}

    public record SchedulerTeamConfigResponse(Long id, Long teamId, String teamName, boolean enabled) {}
}
