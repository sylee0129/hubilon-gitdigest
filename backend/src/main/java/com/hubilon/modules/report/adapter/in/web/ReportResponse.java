package com.hubilon.modules.report.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hubilon.modules.report.application.dto.CommitInfoResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        Long projectId,
        String projectName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        String summary,
        boolean manuallyEdited,
        List<CommitInfoResult> commits,
        int commitCount,
        int contributorCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        boolean aiSummaryFailed
) {}
