package com.hubilon.modules.report.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FolderSummaryResponse(
        Long id,
        Long folderId,
        String folderName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        int totalCommitCount,
        int uniqueContributorCount,
        String summary,
        boolean manuallyEdited,
        boolean aiSummaryFailed,
        String progressSummary,
        String planSummary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {}
