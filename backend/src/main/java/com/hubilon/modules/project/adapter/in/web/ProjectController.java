package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.project.application.dto.ProjectReorderCommand;
import com.hubilon.modules.project.domain.port.in.ProjectDeleteUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectMoveFolderUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectRegisterUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectReorderUseCase;
import com.hubilon.modules.project.domain.port.in.ProjectSearchUseCase;
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

    @Operation(summary = "프로젝트 목록 조회", description = "등록된 GitLab 프로젝트 목록을 반환합니다.")
    @GetMapping
    public Response<List<ProjectSearchResponse>> searchAll() {
        return Response.ok(
                projectSearchUseCase.searchAll().stream()
                        .map(projectWebMapper::toSearchResponse)
                        .toList()
        );
    }

    @Operation(summary = "프로젝트 등록", description = "GitLab 프로젝트를 등록합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<ProjectRegisterResponse> register(@Valid @RequestBody ProjectRegisterRequest request) {
        return Response.ok(
                projectWebMapper.toResponse(
                        projectRegisterUseCase.register(projectWebMapper.toCommand(request))
                )
        );
    }

    @Operation(summary = "프로젝트 삭제", description = "등록된 GitLab 프로젝트를 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response<Void> delete(@PathVariable Long id) {
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
        projectMoveFolderUseCase.moveToFolder(id, request.folderId());
        return Response.ok();
    }
}
