package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.dto.FolderSummaryUpdateCommand;

public interface FolderSummaryUpdateUseCase {

    FolderSummaryResult update(Long id, FolderSummaryUpdateCommand command, String currentUserEmail);
}
