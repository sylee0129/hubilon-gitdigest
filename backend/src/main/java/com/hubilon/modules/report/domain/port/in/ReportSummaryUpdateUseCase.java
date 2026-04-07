package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.ReportSummaryUpdateCommand;
import com.hubilon.modules.report.application.dto.ReportResult;

public interface ReportSummaryUpdateUseCase {

    ReportResult updateSummary(Long reportId, ReportSummaryUpdateCommand command);
}
