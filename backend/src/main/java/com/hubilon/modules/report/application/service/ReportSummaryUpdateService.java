package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.dto.ReportSummaryUpdateCommand;
import com.hubilon.modules.report.application.mapper.ReportAppMapper;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.ReportSummaryUpdateUseCase;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSummaryUpdateService implements ReportSummaryUpdateUseCase {

    private final ReportQueryPort reportQueryPort;
    private final ReportCommandPort reportCommandPort;
    private final ReportAppMapper reportAppMapper;

    @Transactional
    @Override
    public ReportResult updateSummary(Long reportId, ReportSummaryUpdateCommand command) {
        log.info("Updating manual summary for reportId={}", reportId);

        Report report = reportQueryPort.findById(reportId)
                .orElseThrow(() -> new NotFoundException("보고서를 찾을 수 없습니다. id=" + reportId));

        Report updated = report.withManualSummary(command.summary());
        Report saved = reportCommandPort.save(updated);
        return reportAppMapper.toResult(saved);
    }
}
