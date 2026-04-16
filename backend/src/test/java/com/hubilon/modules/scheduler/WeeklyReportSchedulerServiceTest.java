package com.hubilon.modules.scheduler;

import com.hubilon.common.exception.custom.ConflictException;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderCategory;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderQueryUseCase;
import com.hubilon.modules.scheduler.application.service.WeeklyReportProcessor;
import com.hubilon.modules.scheduler.application.service.WeeklyReportSchedulerService;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogCommandPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportSchedulerServiceTest {

    @Mock
    FolderQueryUseCase folderQueryUseCase;

    @Mock
    WeeklyReportProcessor weeklyReportProcessor;

    @Mock
    SchedulerJobLogCommandPort schedulerJobLogCommandPort;

    @Mock
    SchedulerJobLogQueryPort schedulerJobLogQueryPort;

    @InjectMocks
    WeeklyReportSchedulerService weeklyReportSchedulerService;

    @Test
    void RUNNING_잡_존재시_trigger_ConflictException_발생() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(true);

        assertThatThrownBy(() -> weeklyReportSchedulerService.trigger())
                .isInstanceOf(ConflictException.class);

        verify(folderQueryUseCase, never()).searchAll(any());
        verify(schedulerJobLogCommandPort, never()).save(any());
    }

    @Test
    void 모두_성공시_finalizeStatus_SUCCESS() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);

        FolderResult folder = new FolderResult(1L, "개발팀", FolderCategory.DEVELOPMENT, FolderStatus.IN_PROGRESS, 0, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS)).thenReturn(List.of(folder));

        SchedulerJobLog initialLog = SchedulerJobLog.createRunning(1);
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(weeklyReportProcessor.process(eq(folder), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn("https://confluence.example.com/success");

        SchedulerJobLog result = weeklyReportSchedulerService.trigger();

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(0);
    }

    @Test
    void 모두_실패시_finalizeStatus_FAIL() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);

        FolderResult folder = new FolderResult(2L, "기획팀", FolderCategory.NEW_BUSINESS, FolderStatus.IN_PROGRESS, 0, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS)).thenReturn(List.of(folder));
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(weeklyReportProcessor.process(eq(folder), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("Confluence API 오류"));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger();

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.FAIL);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailCount()).isEqualTo(1);
    }

    @Test
    void 일부_성공_일부_실패시_finalizeStatus_PARTIAL_FAIL() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);

        FolderResult folderA = new FolderResult(1L, "개발팀", FolderCategory.DEVELOPMENT, FolderStatus.IN_PROGRESS, 0, null, null);
        FolderResult folderB = new FolderResult(2L, "기획팀", FolderCategory.NEW_BUSINESS, FolderStatus.IN_PROGRESS, 0, null, null);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS)).thenReturn(List.of(folderA, folderB));
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(weeklyReportProcessor.process(eq(folderA), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn("https://confluence.example.com/ok");
        when(weeklyReportProcessor.process(eq(folderB), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("AI 실패"));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger();

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.PARTIAL_FAIL);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
    }

    @Test
    void 폴더_없으면_SUCCESS_상태() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(false);
        when(folderQueryUseCase.searchAll(FolderStatus.IN_PROGRESS)).thenReturn(List.of());
        when(schedulerJobLogCommandPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchedulerJobLog result = weeklyReportSchedulerService.trigger();

        assertThat(result.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
        assertThat(result.getTotalFolderCount()).isEqualTo(0);
        verify(weeklyReportProcessor, never()).process(any(), any(), any());
    }

    @Test
    void RUNNING_잡_존재시_executeWeeklyReport_스킵() {
        when(schedulerJobLogQueryPort.existsByStatus(SchedulerJobStatus.RUNNING)).thenReturn(true);

        weeklyReportSchedulerService.executeWeeklyReport();

        verify(folderQueryUseCase, never()).searchAll(any());
        verify(schedulerJobLogCommandPort, never()).save(any());
    }
}
