package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.FolderSummaryAiPreviewResult;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;

public interface FolderSummaryAiPreviewUseCase {
    FolderSummaryAiPreviewResult preview(FolderSummaryAiSummarizeCommand command);
}
