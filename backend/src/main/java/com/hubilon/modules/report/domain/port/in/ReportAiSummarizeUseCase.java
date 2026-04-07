package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.ReportResult;

public interface ReportAiSummarizeUseCase {

    ReportResult summarize(Long reportId);
}
