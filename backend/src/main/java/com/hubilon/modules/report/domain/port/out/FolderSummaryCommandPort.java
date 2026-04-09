package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.FolderSummary;

public interface FolderSummaryCommandPort {

    FolderSummary save(FolderSummary folderSummary);
}
