package com.hubilon.modules.folder.application.dto;

import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public record FolderCreateCommand(
        String name,
        Long categoryId,
        FolderStatus status,
        List<Long> memberIds,
        Long teamId
) {}
