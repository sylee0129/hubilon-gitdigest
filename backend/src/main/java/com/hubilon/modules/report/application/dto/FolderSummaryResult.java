package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FolderSummaryResult(
        Long id,
        Long folderId,
        String folderName,
        LocalDate startDate,
        LocalDate endDate,
        int totalCommitCount,
        int uniqueContributorCount,
        String summary,
        boolean manuallyEdited,
        boolean aiSummaryFailed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
