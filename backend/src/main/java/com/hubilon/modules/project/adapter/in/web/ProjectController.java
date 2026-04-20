package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.common.exception.custom.ForbiddenException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.common.response.Response;
import com.hubilon.common.security.SecurityUtils;
import com.hubilon.modules.project.application.dto.ProjectReorderCommand;
import com.hubilon.modules.project.domain.port.in.ProjectDeleteUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectMoveFolderUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectRegisterUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectReorderUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectSearchUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.user.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Projects", description = "GitLab 프로젝트 관리 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRegisterUseCase projectRegisterUseCase;
    private final ProjectDeleteUseCase projectDeleteUseCase;
    private final ProjectSearchUseCase projectSearchUseCase;
    private final ProjectReorderUseCase projectReorderUseCase;
    private final ProjectMoveFolderUseCase projectMoveFolderUseCase;
    private final ProjectWebMapper projectWebMapper;
    private final SecurityUtils securityUtils;
    private final ProjectQueryPort projectQueryPort;

    @Operation(summary = "프로젝트 목록 조회", description = "등록된 GitLab 프로젝트 목록을 반환합니다.")
    @GetMapping
    public Response<List<ProjectSearchResponse>> searchAll() {
        User currentUser = securityUtils.getCurrentUser();
        Long teamId = currentUser.getTeamId();
        if (teamId == null) {
            return Response.ok(List.of());
        }
        return Response.ok(
                projectSearchUseCase.searchAll(teamId).stream()
                        .map(projectWebMapper::toSearchResponse)
                        .toList()
        );
    }

    @Operation(summary = "프로젝트 등록", description = "GitLab 프로젝트를 등록합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<ProjectRegisterResponse> register(@Valid @RequestBody ProjectRegisterRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Long teamId = currentUser.getTeamId();
        return Response.ok(
                projectWebMapper.toResponse(
                        projectRegisterUseCase.register(projectWebMapper.toCommand(request, teamId))
                )
        );
    }

    @Operation(summary = "프로젝트 삭제", description = "등록된 GitLab 프로젝트를 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response<Void> delete(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser();
        Long teamId = currentUser.getTeamId();
        projectQueryPort.findById(id)
                .ifPresentOrElse(
                        project -> {
                            if (!teamId.equals(project.getTeamId())) {
                                throw new ForbiddenException("접근 권한이 없습니다.");
                            }
                        },
                        () -> { throw new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id); }
                );
        projectDeleteUseCase.delete(id);
        return Response.ok();
    }

    @Operation(summary = "프로젝트 순서 변경", description = "사이드바 프로젝트 목록 순서를 변경합니다.")
    @PatchMapping("/reorder")
    public Response<Void> reorder(@RequestBody ProjectReorderRequest request) {
        projectReorderUseCase.reorder(new ProjectReorderCommand(request.projectIds()));
        return Response.ok();
    }

    @Operation(summary = "프로젝트 폴더 이동", description = "프로젝트를 특정 폴더로 이동합니다. folderId가 null이면 미분류로 이동합니다.")
    @PatchMapping("/{id}/folder")
    public Response<Void> moveFolder(@PathVariable Long id,
                                      @RequestBody ProjectMoveFolderRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        Long teamId = currentUser.getTeamId();
        projectQueryPort.findById(id)
                .ifPresentOrElse(
                        project -> {
                            if (!teamId.equals(project.getTeamId())) {
                                throw new ForbiddenException("접근 권한이 없습니다.");
                            }
                        },
                        () -> { throw new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id); }
                );
        projectMoveFolderUseCase.moveToFolder(id, request.folderId());
        return Response.ok();
    }
}
