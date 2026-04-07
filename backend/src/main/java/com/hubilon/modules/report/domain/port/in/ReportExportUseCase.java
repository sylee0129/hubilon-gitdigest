package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.ReportExportQuery;

public interface ReportExportUseCase {

    byte[] exportToExcel(ReportExportQuery query);
}
