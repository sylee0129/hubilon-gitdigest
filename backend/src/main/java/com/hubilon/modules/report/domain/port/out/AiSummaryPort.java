package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.CommitInfo;

import java.util.List;

public interface AiSummaryPort {

    String summarize(List<CommitInfo> commits);
}
