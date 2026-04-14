package com.hubilon.modules.report.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FolderSummaryCreateRequest(
        @NotNull Long folderId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String progressSummary,
        String planSummary
) {}
