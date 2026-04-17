package com.hubilon.modules.scheduler.application.service;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.application.port.in.UploadWeeklyReportUseCase;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderQueryUseCase;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerTriggerUseCase;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogCommandPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportSchedulerService implements SchedulerTriggerUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Value("${scheduler.weekly-report.enabled:true}")
    private boolean schedulerEnabled;

    private final FolderQueryUseCase folderQueryUseCase;
    private final WeeklyReportProcessor weeklyReportProcessor;
    private final UploadWeeklyReportUseCase uploadWeeklyReportUseCase;
    private final SchedulerJobLogCommandPort schedulerJobLogCommandPort;
    private final SchedulerJobLogQueryPort schedulerJobLogQueryPort;

    @Scheduled(cron = "${scheduler.weekly-report.cron:0 0 19 * * THU}", zone = "Asia/Seoul")
    @SchedulerLock(name = "weeklyReport", lockAtLeastFor = "PT1H", lockAtMostFor = "PT2H")
    public void executeWeeklyReport() {
        if (!schedulerEnabled) {
            log.info("WeeklyReport scheduler is disabled (scheduler.weekly-report.enabled=false)");
            return;
        }
        if (schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)) {
            log.warn("WeeklyReport scheduler skipped: RUNNING job already exists");
            return;
        }
        runReport();
    }

    @Override
    public SchedulerJobLog trigger() {
        if (schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)) {
            throw new com.hubilon.common.exception.custom.ConflictException(
                    "이미 실행 중인 스케줄러 잡이 있습니다.", null
            );
        }
        return runReport();
    }

    private SchedulerJobLog runReport() {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate startDate = today.with(DayOfWeek.MONDAY);
        LocalDate endDate = today.with(DayOfWeek.SUNDAY);

        List<FolderResult> inProgressFolders = folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS);
        log.info("WeeklyReport started: {} folders, period={} ~ {}", inProgressFolders.size(), startDate, endDate);

        SchedulerJobLog savedLog = schedulerJobLogCommandPort.save(SchedulerJobLog.createRunning(inProgressFolders.size()));

        // 1단계: 각 폴더의 row 데이터 준비 (AI 요약 포함)
        List<WeeklyReportRowDto> successRows = new ArrayList<>();
        for (FolderResult folder : inProgressFolders) {
            try {
                WeeklyReportRowDto row = weeklyReportProcessor.process(folder, startDate, endDate);
                successRows.add(row);
                savedLog.recordSuccess(folder.id(), folder.name(), null); // URL은 업로드 후 채움
            } catch (Exception e) {
                log.error("Row 준비 실패 folderId={}, folderName={}: {}", folder.id(), folder.name(), e.getMessage(), e);
                savedLog.recordFail(folder.id(), folder.name(), e.getMessage());
            }
        }

        // 2단계: 성공한 폴더 전체를 단일 Confluence 업로드
        if (!successRows.isEmpty()) {
            try {
                WeeklyConfluenceRequest request = new WeeklyConfluenceRequest(successRows, startDate.toString(), endDate.toString());
                String confluenceUrl = uploadWeeklyReportUseCase.upload(request);
                log.info("Confluence 업로드 완료: url={}", confluenceUrl);
                savedLog.updateSuccessUrls(confluenceUrl);
            } catch (Exception e) {
                log.error("Confluence 업로드 실패: {}", e.getMessage(), e);
                savedLog.markSuccessAsFailed(e.getMessage());
            }
        }

        savedLog.finalizeStatus();
        SchedulerJobLog finalized = schedulerJobLogCommandPort.save(savedLog);
        log.info("WeeklyReport finished: status={}, success={}, fail={}",
                finalized.getStatus(), finalized.getSuccessCount(), finalized.getFailCount());

        return finalized;
    }
}
