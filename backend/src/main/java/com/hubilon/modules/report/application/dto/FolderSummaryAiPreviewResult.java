package com.hubilon.modules.report.application.dto;

public record FolderSummaryAiPreviewResult(
        String progressSummary,
        String planSummary,
        boolean aiSummaryFailed
) {}
