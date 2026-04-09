package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.FolderSummary;

import java.time.LocalDate;
import java.util.Optional;

public interface FolderSummaryQueryPort {

    Optional<FolderSummary> findById(Long id);

    Optional<FolderSummary> findByFolderIdAndDateRange(Long folderId, LocalDate startDate, LocalDate endDate);
}
