package com.hubilon.modules.folder.adapter.in.web;

import com.hubilon.modules.folder.domain.model.FolderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FolderCreateRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        FolderStatus status,
        List<Long> memberIds
) {}
