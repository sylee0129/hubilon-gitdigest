package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;

public record FolderSummaryQuery(
        Long folderId,
        LocalDate startDate,
        LocalDate endDate
) {}
