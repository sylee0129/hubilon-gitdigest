package com.hubilon.modules.report.application.dto;

import java.time.LocalDate;

public record FolderSummaryCreateCommand(
        Long folderId,
        LocalDate startDate,
        LocalDate endDate,
        String progressSummary,
        String planSummary
) {}
