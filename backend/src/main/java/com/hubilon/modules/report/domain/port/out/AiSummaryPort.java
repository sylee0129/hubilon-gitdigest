package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.AiSummaryResult;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;

import java.time.LocalDate;
import java.util.List;

public interface AiSummaryPort {

    AiSummaryResult summarize(List<CommitInfo> commits);

    FolderAiSummaryResult summarizeFolder(List<CommitInfo> commits, LocalDate startDate, LocalDate endDate, String folderName);
}
