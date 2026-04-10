package com.hubilon.modules.report.adapter.in.web;

public record FolderSummaryUpdateRequest(
        String summary,
        String progressSummary,
        String planSummary
) {}
