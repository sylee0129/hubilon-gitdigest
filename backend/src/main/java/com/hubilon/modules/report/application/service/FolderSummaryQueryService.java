package com.hubilon.modules.report.application.service;

import com.hubilon.modules.report.application.dto.FolderSummaryQuery;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.domain.port.in.FolderSummaryQueryUseCase;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FolderSummaryQueryService implements FolderSummaryQueryUseCase {

    private final FolderSummaryQueryPort folderSummaryQueryPort;
    private final FolderSummaryAppMapper folderSummaryAppMapper;

    @Transactional(readOnly = true)
    @Override
    public Optional<FolderSummaryResult> query(FolderSummaryQuery query) {
        return folderSummaryQueryPort.findByFolderIdAndDateRange(
                        query.folderId(), query.startDate(), query.endDate())
                .map(folderSummaryAppMapper::toResult);
    }
}
