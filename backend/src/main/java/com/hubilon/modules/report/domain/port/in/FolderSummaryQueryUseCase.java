package com.hubilon.modules.report.domain.port.in;

import com.hubilon.modules.report.application.dto.FolderSummaryQuery;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;

import java.util.Optional;

public interface FolderSummaryQueryUseCase {

    Optional<FolderSummaryResult> query(FolderSummaryQuery query);
}
