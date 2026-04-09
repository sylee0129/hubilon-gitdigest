package com.hubilon.modules.folder.application.dto;

import com.hubilon.modules.folder.domain.model.FolderCategory;
import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public record FolderResult(
        Long id,
        String name,
        FolderCategory category,
        FolderStatus status,
        int sortOrder,
        List<FolderMemberResult> members,
        List<WorkProjectResult> workProjects
) {}
