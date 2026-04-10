package com.hubilon.modules.dashboard.adapter.out.persistence;

import java.time.LocalDateTime;

public record ActiveFolderProjection(
        Long folderId,
        String folderName,
        long commitCount,
        LocalDateTime lastCommittedAt
) {
}
