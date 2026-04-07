package com.hubilon.modules.report.adapter.in.web;

import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.dto.ReportSummaryUpdateCommand;
import org.springframework.stereotype.Component;

@Component
public class ReportWebMapper {

    public ReportResponse toResponse(ReportResult result) {
        return new ReportResponse(
                result.id(),
                result.projectId(),
                result.projectName(),
                result.startDate(),
                result.endDate(),
                result.summary(),
                result.manuallyEdited(),
                result.commits(),
                result.commitCount(),
                result.contributorCount(),
                result.createdAt()
        );
    }

    public ReportSummaryUpdateCommand toCommand(ReportSummaryUpdateRequest request) {
        return new ReportSummaryUpdateCommand(request.summary());
    }
}
