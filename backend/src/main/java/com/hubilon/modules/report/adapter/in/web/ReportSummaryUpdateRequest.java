package com.hubilon.modules.report.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record ReportSummaryUpdateRequest(
        @NotBlank(message = "요약 내용은 필수입니다.")
        String summary
) {}
