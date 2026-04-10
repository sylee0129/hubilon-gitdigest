package com.hubilon.modules.dashboard.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResult(
        int totalFolderCount,
        int inProgressFolderCount,
        int todayCommitCount,
        int weeklyCommitCount,
        List<ActiveFolderItem> recentActiveFolders
) {

    public record ActiveFolderItem(
            Long folderId,
            String folderName,
            int commitCount,
            LocalDateTime lastCommittedAt
    ) {
    }
}
