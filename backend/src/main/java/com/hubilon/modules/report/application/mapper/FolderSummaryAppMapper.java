package com.hubilon.modules.report.application.mapper;

import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.domain.model.FolderSummary;
import org.springframework.stereotype.Component;

@Component
public class FolderSummaryAppMapper {

    public FolderSummaryResult toResult(FolderSummary folderSummary) {
        return new FolderSummaryResult(
                folderSummary.getId(),
                folderSummary.getFolderId(),
                folderSummary.getFolderName(),
                folderSummary.getStartDate(),
                folderSummary.getEndDate(),
                folderSummary.getTotalCommitCount(),
                folderSummary.getUniqueContributorCount(),
                folderSummary.getSummary(),
                folderSummary.isManuallyEdited(),
                folderSummary.isAiSummaryFailed(),
                folderSummary.getProgressSummary(),
                folderSummary.getPlanSummary(),
                folderSummary.getCreatedAt(),
                folderSummary.getUpdatedAt()
        );
    }
}
