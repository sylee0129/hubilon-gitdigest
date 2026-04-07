package com.hubilon.modules.report.application.service;

import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.dto.ReportSearchQuery;
import com.hubilon.modules.report.application.mapper.ReportAppMapper;
import com.hubilon.modules.report.domain.port.in.ReportSearchUseCase;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportSearchService implements ReportSearchUseCase {

    private final ReportQueryPort reportQueryPort;
    private final ReportAppMapper reportAppMapper;

    @Transactional(readOnly = true)
    @Override
    public List<ReportResult> search(ReportSearchQuery query) {
        List<com.hubilon.modules.report.domain.model.Report> reports;

        if (query.projectId() != null) {
            reports = reportQueryPort.findByProjectIdAndDateRange(
                    query.projectId(), query.startDate(), query.endDate());
        } else {
            reports = reportQueryPort.findByDateRange(query.startDate(), query.endDate());
        }

        return reports.stream()
                .map(reportAppMapper::toResult)
                .toList();
    }
}
