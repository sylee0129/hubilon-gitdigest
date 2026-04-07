package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.ReportSearchQuery;
import com.hubilon.modules.report.application.dto.ReportResult;

import java.util.List;

public interface ReportSearchUseCase {

    List<ReportResult> search(ReportSearchQuery query);
}
