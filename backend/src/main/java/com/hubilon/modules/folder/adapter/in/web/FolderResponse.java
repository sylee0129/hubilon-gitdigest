package com.hubilon.modules.folder.adapter.in.web;

import com.hubilon.modules.folder.application.dto.FolderMemberResult;
import com.hubilon.modules.folder.application.dto.WorkProjectResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public record FolderResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        FolderStatus status,
        int sortOrder,
        Long teamId,
        List<FolderMemberResult> members,
        List<WorkProjectResult> workProjects
) {}
