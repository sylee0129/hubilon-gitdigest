package com.hubilon.modules.report.adapter.in.web;

public record FolderSummaryAiPreviewResponse(
        String progressSummary,
        String planSummary,
        boolean aiSummaryFailed
) {}
