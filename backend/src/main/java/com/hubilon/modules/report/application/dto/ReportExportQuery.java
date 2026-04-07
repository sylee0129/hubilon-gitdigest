package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;

public record ReportExportQuery(
        Long projectId,
        LocalDate startDate,
        LocalDate endDate
) {}
