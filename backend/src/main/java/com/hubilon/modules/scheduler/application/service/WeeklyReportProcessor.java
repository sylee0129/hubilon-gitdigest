package com.hubilon.modules.scheduler.application.service;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.scheduler.application.service.strategy.AiUploadStrategy;
import com.hubilon.modules.scheduler.application.service.strategy.ManualUploadStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportProcessor {

    private final FolderSummaryQueryPort folderSummaryQueryPort;
    private final ManualUploadStrategy manualUploadStrategy;
    private final AiUploadStrategy aiUploadStrategy;

    /**
     * 폴더 1개에 대한 주간보고 row 데이터를 준비한다.
     * Confluence 업로드는 스케줄러가 모든 폴더 처리 후 일괄 수행한다.
     *
     * @return Confluence 테이블에 들어갈 행 데이터
     */
    public WeeklyReportRowDto process(FolderResult folder, LocalDate startDate, LocalDate endDate) {
        FolderSummary existing = folderSummaryQueryPort
                .findByFolderIdAndDateRange(folder.id(), startDate, endDate)
                .orElse(null);

        boolean hasManualContent = existing != null
                && hasContent(existing.getProgressSummary())
                && hasContent(existing.getPlanSummary());

        if (hasManualContent) {
            log.info("Manual path: folderId={}, folderName={}", folder.id(), folder.name());
            return manualUploadStrategy.execute(folder, existing, startDate, endDate);
        }

        log.info("AI path: folderId={}, folderName={}", folder.id(), folder.name());
        return aiUploadStrategy.execute(folder, existing, startDate, endDate);
    }

    private boolean hasContent(String value) {
        return value != null && !value.isBlank();
    }
}
