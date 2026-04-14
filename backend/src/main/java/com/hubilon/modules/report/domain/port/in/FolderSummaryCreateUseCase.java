package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.FolderSummaryCreateCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;

public interface FolderSummaryCreateUseCase {
    FolderSummaryResult create(FolderSummaryCreateCommand command);
}
