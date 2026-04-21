package com.hubilon.modules.confluence.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.common.security.SecurityUtils;
import com.hubilon.modules.confluence.application.dto.SpaceConfigResponse;
import com.hubilon.modules.confluence.application.dto.TeamConfigResponse;
import com.hubilon.modules.confluence.application.service.ConfluenceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Confluence Admin", description = "Confluence 연동 설정 관리 API (ADMIN 전용)")
@RestController
@RequestMapping("/api/admin/confluence")
@RequiredArgsConstructor
public class ConfluenceConfigController {

    private final ConfluenceConfigService confluenceConfigService;
    private final SecurityUtils securityUtils;

    // ===== Space 엔드포인트 =====

    @Operation(summary = "Space 설정 목록 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/spaces")
    public Response<List<SpaceConfigResponse>> getSpaceConfigs() {
        return Response.ok(confluenceConfigService.getSpaceConfigs());
    }

    @Operation(summary = "Space 설정 등록/수정 (upsert)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/spaces")
    public Response<SpaceConfigResponse> upsertSpaceConfig(
            @Valid @RequestBody SpaceConfigUpsertRequest request) {
        String currentUserEmail = securityUtils.getCurrentUser().getEmail();
        SpaceConfigResponse result = confluenceConfigService.upsertSpaceConfig(
                request.deptId(),
                request.userEmail(),
                request.apiToken(),
                request.spaceKey(),
                request.baseUrl(),
                currentUserEmail
        );
        return Response.ok(result);
    }

    @Operation(summary = "Space 설정 삭제")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/spaces/{deptId}")
    public Response<Void> deleteSpaceConfig(@PathVariable Long deptId) {
        String currentUserEmail = securityUtils.getCurrentUser().getEmail();
        confluenceConfigService.deleteSpaceConfig(deptId, currentUserEmail);
        return Response.ok();
    }

    @Operation(summary = "Space 연결 테스트 (저장된 설정 기준)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/spaces/{deptId}/test")
    public Response<Map<String, String>> testConnection(@PathVariable Long deptId) {
        confluenceConfigService.testConnection(deptId);
        return Response.ok(Map.of("result", "연결 성공"));
    }

    @Operation(summary = "Space 연결 테스트 (폼 입력값 직접)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/spaces/test")
    public Response<Map<String, String>> testConnectionDirect(
            @Valid @RequestBody SpaceConfigTestRequest request) {
        confluenceConfigService.testConnectionDirect(
                request.userEmail(),
                request.apiToken(),
                request.spaceKey(),
                request.baseUrl()
        );
        return Response.ok(Map.of("result", "연결 성공"));
    }

    // ===== Team 엔드포인트 =====

    @Operation(summary = "Team 설정 목록 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/teams")
    public Response<List<TeamConfigResponse>> getTeamConfigs() {
        return Response.ok(confluenceConfigService.getTeamConfigs());
    }

    @Operation(summary = "Team 설정 등록/수정 (upsert)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/teams")
    public Response<TeamConfigResponse> upsertTeamConfig(
            @Valid @RequestBody TeamConfigUpsertRequest request) {
        String currentUserEmail = securityUtils.getCurrentUser().getEmail();
        TeamConfigResponse result = confluenceConfigService.upsertTeamConfig(
                request.teamId(),
                request.parentPageId(),
                currentUserEmail
        );
        return Response.ok(result);
    }

    @Operation(summary = "Team 설정 삭제")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/teams/{teamId}")
    public Response<Void> deleteTeamConfig(@PathVariable Long teamId) {
        confluenceConfigService.deleteTeamConfig(teamId);
        return Response.ok();
    }
}
