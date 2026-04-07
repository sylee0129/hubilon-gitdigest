package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;

public record ReportSearchQuery(
        Long projectId,
        LocalDate startDate,
        LocalDate endDate
) {}
