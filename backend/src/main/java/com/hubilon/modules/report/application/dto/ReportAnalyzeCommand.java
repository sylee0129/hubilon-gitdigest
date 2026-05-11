package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportAnalyzeCommand(
        Long projectId,
        List<Long> projectIds,
        LocalDate startDate,
        LocalDate endDate,
        Long teamId,
        boolean forceRefresh
) {
    public ReportAnalyzeCommand(Long projectId, LocalDate startDate, LocalDate endDate) {
        this(projectId, null, startDate, endDate, null, false);
    }

    public ReportAnalyzeCommand(Long projectId, List<Long> projectIds, LocalDate startDate, LocalDate endDate) {
        this(projectId, projectIds, startDate, endDate, null, false);
    }
}
