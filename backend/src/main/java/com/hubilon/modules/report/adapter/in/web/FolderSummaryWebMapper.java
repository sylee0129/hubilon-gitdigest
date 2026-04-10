package com.hubilon.modules.report.adapter.in.web;

import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.dto.FolderSummaryUpdateCommand;
import org.springframework.stereotype.Component;

@Component
public class FolderSummaryWebMapper {

    public FolderSummaryResponse toResponse(FolderSummaryResult result) {
        return new FolderSummaryResponse(
                result.id(),
                result.folderId(),
                result.folderName(),
                result.startDate(),
                result.endDate(),
                result.totalCommitCount(),
                result.uniqueContributorCount(),
                result.summary(),
                result.manuallyEdited(),
                result.aiSummaryFailed(),
                result.progressSummary(),
                result.planSummary(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public FolderSummaryAiSummarizeCommand toCommand(FolderSummaryAiSummarizeRequest request) {
        return new FolderSummaryAiSummarizeCommand(
                request.folderId(),
                request.startDate(),
                request.endDate()
        );
    }

    public FolderSummaryUpdateCommand toCommand(FolderSummaryUpdateRequest request) {
        return new FolderSummaryUpdateCommand(
                request.summary(),
                request.progressSummary(),
                request.planSummary()
        );
    }
}
