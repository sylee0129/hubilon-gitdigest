package com.hubilon.modules.scheduler;

import com.hubilon.common.exception.custom.ConflictException;
import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.confluence.application.port.in.UploadWeeklyReportUseCase;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderQueryUseCase;
import com.hubilon.modules.scheduler.application.service.WeeklyReportProcessor;
import com.hubilon.modules.scheduler.application.service.WeeklyReportSchedulerService;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogCommandPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerTeamConfigPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportSchedulerServiceTest {

    @Mock FolderQueryUseCase folderQueryUseCase;
    @Mock WeeklyReportProcessor weeklyReportProcessor;
    @Mock UploadWeeklyReportUseCase uploadWeeklyReportUseCase;
    @Mock SchedulerJobLogCommandPort schedulerJobLogCommandPort;
    @Mock SchedulerJobLogQueryPort schedulerJobLogQueryPort;
    @Mock SchedulerTeamConfigPort schedulerTeamConfigPort;
    @Mock TeamQueryPort teamQueryPort;

    @InjectMocks
    WeeklyReportSchedulerService weeklyReportSchedulerService;

    private static final Long TEAM_ID = 1L;
    private static final String TEAM_NAME = "개발팀";

    private Team team() {
        return Team.builder().id(TEAM_ID).name(TEAM_NAME).deptId(10L).build();
    }

    private WeeklyReportRowDto sampleRow() {
        return new WeeklyReportRowDto(1L, TEAM_ID, "개발사업", "개발팀 폴더", List.of(), "진행 내용", "계획 내용");
    }

    @Test
    void RUNNING_잡_존재시_trigger_ConflictException_발생() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> weeklyReportSchedulerService.trigger(TEAM_ID))
                .isInstanceOf(ConflictException.class);

        verify(folderQueryUseCase, never()).searchAll(any(), any());
        verify(schedulerJobLogCommandPort, never()).save(any());
    }

    @Test
    void 존재하지_않는_팀_trigger시_IllegalArgumentException_발생() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weeklyReportSchedulerService.trigger(TEAM_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("팀을 찾을 수 없습니다");

        verify(folderQueryUseCase, never()).searchAll(any(), any());
    }

    @Test
    void 모두_성공시_finalizeStatus_SUCCESS() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        FolderResult folder = new FolderResult(1L, "개발팀 폴더", TEAM_ID, TEAM_NAME, FolderStatus.IN_PROGRESS, 0, TEAM_ID, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, TEAM_ID)).thenReturn(List.of(folder));
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(weeklyReportProcessor.process(eq(folder), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(sampleRow());
        when(uploadWeeklyReportUseCase.upload(any())).thenReturn("https://confluence.example.com/success");

        SchedulerJobLog result = weeklyReportSchedulerService.trigger(TEAM_ID);

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(0);
        assertThat(result.getTeamId()).isEqualTo(TEAM_ID);
        assertThat(result.getTeamName()).isEqualTo(TEAM_NAME);
    }

    @Test
    void 모두_실패시_finalizeStatus_FAIL() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        FolderResult folder = new FolderResult(2L, "기획팀 폴더", TEAM_ID, TEAM_NAME, FolderStatus.IN_PROGRESS, 0, TEAM_ID, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, TEAM_ID)).thenReturn(List.of(folder));
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(weeklyReportProcessor.process(eq(folder), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Confluence API 오류"));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger(TEAM_ID);

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.FAIL);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailCount()).isEqualTo(1);
    }

    @Test
    void 일부_성공_일부_실패시_finalizeStatus_PARTIAL_FAIL() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));

        FolderResult folderA = new FolderResult(1L, "폴더A", TEAM_ID, TEAM_NAME, FolderStatus.IN_PROGRESS, 0, TEAM_ID, null, null);
        FolderResult folderB = new FolderResult(2L, "폴더B", TEAM_ID, TEAM_NAME, FolderStatus.IN_PROGRESS, 0, TEAM_ID, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, TEAM_ID)).thenReturn(List.of(folderA, folderB));
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(uploadWeeklyReportUseCase.upload(any())).thenReturn("https://confluence.example.com/ok");

        when(weeklyReportProcessor.process(eq(folderA), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(sampleRow());
        when(weeklyReportProcessor.process(eq(folderB), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("AI 실패"));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger(TEAM_ID);

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.PARTIAL_FAIL);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
    }

    @Test
    void 폴더_없으면_SUCCESS_상태() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS, TEAM_ID)).thenReturn(List.of());
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger(TEAM_ID);

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
        assertThat(result.getTotalFolderCount()).isEqualTo(0);
        verify(weeklyReportProcessor, never()).process(any(), any(), any());
    }

    @Test
    void executeWeeklyReport_disabled_시_아무것도_실행안함() {
        // schedulerEnabled 기본값 false — 아무 의존성도 호출되지 않아야 함
        weeklyReportSchedulerService.executeWeeklyReport();

        verify(folderQueryUseCase, never()).searchAll(any(), any());
        verify(schedulerJobLogCommandPort, never()).save(any());
    }
}