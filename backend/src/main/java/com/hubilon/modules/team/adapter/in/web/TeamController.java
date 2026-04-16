package com.hubilon.modules.team.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Teams", description = "팀 관리 API")
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamQueryPort teamQueryPort;

    @Operation(summary = "팀 목록 조회")
    @GetMapping
    public Response<List<TeamResponse>> findAll() {
        List<TeamResponse> teams = teamQueryPort.findAll().stream()
                .map(team -> new TeamResponse(team.getId(), team.getName()))
                .toList();
        return Response.ok(teams);
    }
}
