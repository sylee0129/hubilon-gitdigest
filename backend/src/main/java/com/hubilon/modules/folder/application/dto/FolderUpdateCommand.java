package com.hubilon.modules.folder.application.dto;

import com.hubilon.modules.folder.domain.model.FolderCategory;
import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public record FolderUpdateCommand(
        String name,
        FolderCategory category,
        FolderStatus status,
        List<Long> memberIds
) {}
