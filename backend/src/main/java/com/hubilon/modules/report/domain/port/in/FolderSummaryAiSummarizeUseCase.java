package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;

public interface FolderSummaryAiSummarizeUseCase {

    FolderSummaryResult summarize(FolderSummaryAiSummarizeCommand command);
}
