package com.hubilon.modules.folder.application.dto;

import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public record FolderResult(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        FolderStatus status,
        int sortOrder,
        List<FolderMemberResult> members,
        List<WorkProjectResult> workProjects
) {}
