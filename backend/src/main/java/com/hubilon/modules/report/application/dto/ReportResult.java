package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportResult(
        Long id,
        Long projectId,
        String projectName,
        LocalDate startDate,
        LocalDate endDate,
        String summary,
        boolean manuallyEdited,
        List<CommitInfoResult> commits,
        int commitCount,
        int contributorCount,
        LocalDateTime createdAt
) {}
