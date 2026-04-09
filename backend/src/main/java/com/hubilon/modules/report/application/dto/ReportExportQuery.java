package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ReportExportQuery(
        List<Long> projectIds,
        LocalDate startDate,
        LocalDate endDate
) {}
