package com.hubilon.modules.confluence.adapter.in.web;

import java.util.List;

public record WeeklyConfluenceRequest(
        List<WeeklyReportRowDto> rows,
        String startDate,
        String endDate
) {
    public record WeeklyReportRowDto(
            String category,
            String folderName,
            List<String> members,
            String progressSummary,
            String planSummary
    ) {}
}
