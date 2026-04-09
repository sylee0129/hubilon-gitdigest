package com.hubilon.modules.workproject.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.workproject.application.dto.WorkProjectCreateCommand;
import com.hubilon.modules.workproject.application.dto.WorkProjectReorderCommand;
import com.hubilon.modules.workproject.application.dto.WorkProjectResult;
import com.hubilon.modules.workproject.application.dto.WorkProjectUpdateCommand;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectCreateUseCase;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectDeleteUseCase;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectReorderUseCase;
import com.hubilon.modules.workproject.domain.port.in.WorkProjectUpdateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "WorkProjects", description = "세부 프로젝트 관리 API")
@RestController
@RequestMapping("/api/work-projects")
@RequiredArgsConstructor
public class WorkProjectController {

    private final WorkProjectCreateUseCase workProjectCreateUseCase;
    private final WorkProjectUpdateUseCase workProjectUpdateUseCase;
    private final WorkProjectDeleteUseCase workProjectDeleteUseCase;
    private final WorkProjectReorderUseCase workProjectReorderUseCase;

    @Operation(summary = "세부 프로젝트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<WorkProjectResponse> create(@Valid @RequestBody WorkProjectCreateRequest req) {
        WorkProjectResult result = workProjectCreateUseCase.create(new WorkProjectCreateCommand(req.folderId(), req.name()));
        return Response.ok(new WorkProjectResponse(result.id(), result.folderId(), result.name(), result.sortOrder()));
    }

    @Operation(summary = "세부 프로젝트 수정")
    @PutMapping("/{id}")
    public Response<WorkProjectResponse> update(@PathVariable Long id, @Valid @RequestBody WorkProjectUpdateRequest req) {
        WorkProjectResult result = workProjectUpdateUseCase.update(id, new WorkProjectUpdateCommand(req.folderId(), req.name()));
        return Response.ok(new WorkProjectResponse(result.id(), result.folderId(), result.name(), result.sortOrder()));
    }

    @Operation(summary = "세부 프로젝트 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response<Void> delete(@PathVariable Long id) {
        workProjectDeleteUseCase.delete(id);
        return Response.ok();
    }

    @Operation(summary = "세부 프로젝트 순서 변경")
    @PatchMapping("/reorder")
    public Response<Void> reorder(@RequestBody WorkProjectReorderRequest req) {
        workProjectReorderUseCase.reorder(new WorkProjectReorderCommand(
                req.folderId(),
                req.orders().stream()
                        .map(o -> new WorkProjectReorderCommand.OrderItem(o.id(), o.sortOrder()))
                        .toList()
        ));
        return Response.ok();
    }
}
