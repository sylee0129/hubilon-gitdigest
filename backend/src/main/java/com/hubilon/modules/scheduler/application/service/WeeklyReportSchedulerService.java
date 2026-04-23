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
import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;
import com.hubilon.modules.scheduler.domain.port.in.SchedulerTriggerUseCase;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogCommandPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerTeamConfigPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
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
    private final SchedulerTeamConfigPort schedulerTeamConfigPort;
    private final TeamQueryPort teamQueryPort;

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

        List<SchedulerTeamConfig> enabledTeams = schedulerTeamConfigPort.findAllByEnabled(true);
        for (SchedulerTeamConfig teamConfig : enabledTeams) {
            triggerForTeam(teamConfig.teamId(), teamConfig.teamName());
        }
    }

    @Override
    public SchedulerJobLog trigger(Long teamId) {
        if (schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)) {
            throw new ConflictException("이미 실행 중인 주간보고 스케줄러가 있습니다.", null);
        }

        Team team = teamQueryPort.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다. teamId=" + teamId));

        return triggerForTeam(teamId, team.getName());
    }

    private SchedulerJobLog triggerForTeam(Long teamId, String teamName) {
        List<FolderResult> folders = folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, teamId);

        SchedulerJobLog jobLog = schedulerJobLogCommandPort.save(
                SchedulerJobLog.createRunning(teamId, teamName, folders.size())
        );

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gitStartDate = monday;                // 월요일 (프론트엔드와 동일)
        LocalDate gitEndDate = monday.plusDays(6);      // 일요일 (프론트엔드와 동일)

        // teamId → rows 그룹핑
        Map<Long, List<WeeklyReportRowDto>> rowsByTeam = new LinkedHashMap<>();

        for (FolderResult folder : folders) {
            try {
                WeeklyReportRowDto row = weeklyReportProcessor.process(folder, gitStartDate, gitEndDate);
                Long foldersTeamId = folder.teamId();
                if (foldersTeamId != null) {
                    rowsByTeam.computeIfAbsent(foldersTeamId, k -> new ArrayList<>()).add(row);
                }
                jobLog.recordSuccess(folder.id(), folder.name(), null);
            } catch (Exception e) {
                log.warn("폴더 처리 실패: folderId={}, folderName={}, error={}", folder.id(), folder.name(), e.getMessage());
                jobLog.recordFail(folder.id(), folder.name(), e.getMessage());
            }
        }

        // 팀별 Confluence 업로드
        for (Map.Entry<Long, List<WeeklyReportRowDto>> entry : rowsByTeam.entrySet()) {
            Long entryTeamId = entry.getKey();
            List<WeeklyReportRowDto> successRows = entry.getValue();
            if (successRows.isEmpty()) continue;
            try {
                WeeklyConfluenceRequest confluenceRequest = new WeeklyConfluenceRequest(
                        entryTeamId, successRows, gitStartDate.toString(), gitEndDate.toString());
                String pageUrl = uploadWeeklyReportUseCase.upload(confluenceRequest);
                jobLog.updateSuccessUrls(pageUrl);
                log.info("Confluence 업로드 성공: teamId={}, pageUrl={}", entryTeamId, pageUrl);
            } catch (Exception e) {
                log.error("Confluence 업로드 실패: teamId={}, error={}", entryTeamId, e.getMessage());
                jobLog.markSuccessAsFailed(e.getMessage());
            }
        }

        jobLog.finalizeStatus();
        return schedulerJobLogCommandPort.save(jobLog);
    }
}
