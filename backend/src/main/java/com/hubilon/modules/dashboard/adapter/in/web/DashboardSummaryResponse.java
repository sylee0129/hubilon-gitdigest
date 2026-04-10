package com.hubilon.modules.dashboard.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hubilon.modules.dashboard.application.dto.DashboardSummaryResult;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        int totalFolderCount,
        int inProgressFolderCount,
        int todayCommitCount,
        int weeklyCommitCount,
        List<RecentActiveFolderItem> recentActiveFolders
) {

    public record RecentActiveFolderItem(
            Long folderId,
            String folderName,
            int commitCount,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime lastCommittedAt
    ) {
    }

    public static DashboardSummaryResponse from(DashboardSummaryResult result) {
        List<RecentActiveFolderItem> items = result.recentActiveFolders().stream()
                .map(item -> new RecentActiveFolderItem(
                        item.folderId(),
                        item.folderName(),
                        item.commitCount(),
                        item.lastCommittedAt()
                ))
                .toList();

        return new DashboardSummaryResponse(
                result.totalFolderCount(),
                result.inProgressFolderCount(),
                result.todayCommitCount(),
                result.weeklyCommitCount(),
                items
        );
    }
}
