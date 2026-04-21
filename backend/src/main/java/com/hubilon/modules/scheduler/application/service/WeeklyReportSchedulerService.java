package com.hubilon.modules.scheduler.application.service;

import com.hubilon.common.exception.custom.ConflictException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportSchedulerService implements SchedulerTriggerUseCase {

    @Value("${scheduler.weekly-report.enabled:false}")
    private boolean schedulerEnabled;

    private final FolderQueryUseCase folderQueryUseCase;
    private final WeeklyReportProcessor weeklyReportProcessor;
    private final UploadWeeklyReportUseCase uploadWeeklyReportUseCase;
    private final SchedulerJobLogCommandPort schedulerJobLogCommandPort;
    private final SchedulerJobLogQueryPort schedulerJobLogQueryPort;

    @Scheduled(cron = "${scheduler.weekly-report.cron:0 0 9 * * MON}")
    public void executeWeeklyReport() {
        if (!schedulerEnabled) {
            log.debug("Weekly report scheduler is disabled. Skipping.");
            return;
        }
        if (schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)) {
            log.warn("Weekly report scheduler skipped: another job is already RUNNING.");
            return;
        }
        trigger();
    }

    @Override
    public SchedulerJobLog trigger() {
        if (schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)) {
            throw new ConflictException("이미 실행 중인 주간보고 스케줄러가 있습니다.", null);
        }

        List<FolderResult> folders = folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, null);

        SchedulerJobLog jobLog = schedulerJobLogCommandPort.save(
                SchedulerJobLog.createRunning(folders.size())
        );

        LocalDate endDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        LocalDate startDate = endDate.minusDays(6);

        // teamId → rows 그룹핑
        Map<Long, List<WeeklyReportRowDto>> rowsByTeam = new LinkedHashMap<>();

        for (FolderResult folder : folders) {
            try {
                WeeklyReportRowDto row = weeklyReportProcessor.process(folder, startDate, endDate);
                Long teamId = folder.teamId();
                if (teamId != null) {
                    rowsByTeam.computeIfAbsent(teamId, k -> new ArrayList<>()).add(row);
                }
                jobLog.recordSuccess(folder.id(), folder.name(), null);
            } catch (Exception e) {
                log.warn("폴더 처리 실패: folderId={}, folderName={}, error={}", folder.id(), folder.name(), e.getMessage());
                jobLog.recordFail(folder.id(), folder.name(), e.getMessage());
            }
        }

        // 팀별 Confluence 업로드
        for (Map.Entry<Long, List<WeeklyReportRowDto>> entry : rowsByTeam.entrySet()) {
            Long teamId = entry.getKey();
            List<WeeklyReportRowDto> successRows = entry.getValue();
            if (successRows.isEmpty()) continue;
            try {
                WeeklyConfluenceRequest confluenceRequest = new WeeklyConfluenceRequest(
                        teamId, successRows, startDate.toString(), endDate.toString());
                String pageUrl = uploadWeeklyReportUseCase.upload(confluenceRequest);
                jobLog.updateSuccessUrls(pageUrl);
                log.info("Confluence 업로드 성공: teamId={}, pageUrl={}", teamId, pageUrl);
            } catch (Exception e) {
                log.error("Confluence 업로드 실패: teamId={}, error={}", teamId, e.getMessage());
                jobLog.markSuccessAsFailed(e.getMessage());
            }
        }

        jobLog.finalizeStatus();
        return schedulerJobLogCommandPort.save(jobLog);
    }
}
