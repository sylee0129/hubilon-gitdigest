package com.hubilon.modules.report.domain.model;

public record FolderAiSummaryResult(
        String progressSummary,
        String planSummary,
        boolean aiUsed
) {}
