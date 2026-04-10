package com.hubilon.modules.report.application.dto;

public record FolderSummaryUpdateCommand(
        String summary,
        String progressSummary,
        String planSummary
) {}
