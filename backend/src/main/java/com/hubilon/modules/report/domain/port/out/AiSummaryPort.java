package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.AiSummaryResult;
import com.hubilon.modules.report.domain.model.CommitInfo;

import java.util.List;

public interface AiSummaryPort {

    AiSummaryResult summarize(List<CommitInfo> commits);
}
