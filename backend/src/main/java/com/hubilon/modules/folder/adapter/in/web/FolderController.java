package com.hubilon.modules.folder.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderCreateUseCase;
import com.hubilon.modules.folder.domain.port.in.FolderDeleteUseCase;
import com.hubilon.modules.folder.domain.port.in.FolderQueryUseCase;
import com.hubilon.modules.folder.domain.port.in.FolderReorderUseCase;
import com.hubilon.modules.folder.domain.port.in.FolderUpdateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Folders", description = "프로젝트 폴더 관리 API")
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderCreateUseCase folderCreateUseCase;
    private final FolderUpdateUseCase folderUpdateUseCase;
    private final FolderDeleteUseCase folderDeleteUseCase;
    private final FolderQueryUseCase folderQueryUseCase;
    private final FolderReorderUseCase folderReorderUseCase;
    private final FolderWebMapper folderWebMapper;

    @Operation(summary = "폴더 목록 조회")
    @GetMapping
    public Response<List<FolderResponse>> searchAll(@RequestParam(required = false) FolderStatus status) {
        return Response.ok(
                folderQueryUseCase.searchAll(status).stream()
                        .map(folderWebMapper::toResponse)
                        .toList()
        );
    }

    @Operation(summary = "폴더 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<FolderResponse> create(@Valid @RequestBody FolderCreateRequest request) {
        return Response.ok(folderWebMapper.toResponse(
                folderCreateUseCase.create(folderWebMapper.toCreateCommand(request))
        ));
    }

    @Operation(summary = "폴더 수정")
    @PutMapping("/{id}")
    public Response<FolderResponse> update(@PathVariable Long id, @Valid @RequestBody FolderUpdateRequest request) {
        return Response.ok(folderWebMapper.toResponse(
                folderUpdateUseCase.update(id, folderWebMapper.toUpdateCommand(request))
        ));
    }

    @Operation(summary = "폴더 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response<Void> delete(@PathVariable Long id,
                                  @RequestParam(defaultValue = "false") boolean force) {
        folderDeleteUseCase.delete(id, force);
        return Response.ok();
    }

    @Operation(summary = "폴더 순서 변경")
    @PatchMapping("/reorder")
    public Response<Void> reorder(@RequestBody FolderReorderRequest request) {
        folderReorderUseCase.reorder(folderWebMapper.toReorderCommand(request));
        return Response.ok();
    }
}
