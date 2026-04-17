package com.hubilon.modules.category.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.category.application.dto.CategoryCreateCommand;
import com.hubilon.modules.category.application.dto.CategoryUpdateCommand;
import com.hubilon.modules.category.domain.port.in.CategoryCreateUseCase;
import com.hubilon.modules.category.domain.port.in.CategoryQueryUseCase;
import com.hubilon.modules.category.domain.port.in.CategoryUpdateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryQueryUseCase categoryQueryUseCase;
    private final CategoryCreateUseCase categoryCreateUseCase;
    private final CategoryUpdateUseCase categoryUpdateUseCase;

    @Operation(summary = "카테고리 전체 조회")
    @GetMapping
    public Response<List<CategoryResponse>> findAll() {
        return Response.ok(
                categoryQueryUseCase.findAll().stream()
                        .map(r -> new CategoryResponse(r.id(), r.name(), r.sortOrder()))
                        .toList()
        );
    }

    @Operation(summary = "카테고리 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Response<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        var result = categoryCreateUseCase.create(new CategoryCreateCommand(request.name()));
        return Response.ok(new CategoryResponse(result.id(), result.name(), result.sortOrder()));
    }

    @Operation(summary = "카테고리 수정")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<CategoryResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody CategoryUpdateRequest request) {
        var result = categoryUpdateUseCase.update(id, new CategoryUpdateCommand(request.name()));
        return Response.ok(new CategoryResponse(result.id(), result.name(), result.sortOrder()));
    }
}
