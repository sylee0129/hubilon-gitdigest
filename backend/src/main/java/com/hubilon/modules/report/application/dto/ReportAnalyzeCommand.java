package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportAnalyzeCommand(
        Long projectId,
        List<Long> projectIds,
        LocalDate startDate,
        LocalDate endDate,
        Long teamId
) {
    public ReportAnalyzeCommand(Long projectId, LocalDate startDate, LocalDate endDate) {
        this(projectId, null, startDate, endDate, null);
    }

    public ReportAnalyzeCommand(Long projectId, List<Long> projectIds, LocalDate startDate, LocalDate endDate) {
        this(projectId, projectIds, startDate, endDate, null);
    }
}
