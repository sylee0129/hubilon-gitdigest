package com.hubilon.modules.folder.adapter.in.web;

import com.hubilon.modules.folder.domain.model.FolderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FolderUpdateRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        @NotNull FolderStatus status,
        List<Long> memberIds,
        Long teamId
) {}
