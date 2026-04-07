package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.ReportAnalyzeCommand;
import com.hubilon.modules.report.application.dto.ReportResult;

import java.util.List;

public interface ReportAnalyzeUseCase {

    List<ReportResult> analyze(ReportAnalyzeCommand command);
}
