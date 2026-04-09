package com.hubilon.modules.report.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record FolderSummaryUpdateRequest(
        @NotBlank String summary
) {}
