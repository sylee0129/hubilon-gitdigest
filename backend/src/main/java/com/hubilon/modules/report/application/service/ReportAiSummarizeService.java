package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.mapper.ReportAppMapper;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.ReportAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAiSummarizeService implements ReportAiSummarizeUseCase {

    private final ReportQueryPort reportQueryPort;
    private final ReportCommandPort reportCommandPort;
    private final AiSummaryPort aiSummaryPort;
    private final ReportAppMapper reportAppMapper;

    @Transactional
    @Override
    public ReportResult summarize(Long reportId) {
        Report report = reportQueryPort.findById(reportId)
                .orElseThrow(() -> new NotFoundException("보고서를 찾을 수 없습니다. id=" + reportId));

        log.info("Generating AI summary for reportId={}", reportId);
        String aiSummary = aiSummaryPort.summarize(report.getCommits());

        Report updated = report.withAiSummary(aiSummary);
        return reportAppMapper.toResult(reportCommandPort.save(updated));
    }
}
