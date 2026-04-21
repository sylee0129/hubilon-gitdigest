package com.hubilon.modules.confluence.adapter.in.web;

import java.util.List;

public record WeeklyConfluenceRequest(
        Long teamId,
        List<WeeklyReportRowDto> rows,
        String startDate,
        String endDate
) {
    public record WeeklyReportRowDto(
            Long categoryId,
            String categoryName,
            String folderName,
            List<String> members,
            String progressSummary,
            String planSummary
    ) {}
}
