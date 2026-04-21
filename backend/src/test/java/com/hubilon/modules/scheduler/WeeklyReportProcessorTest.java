package com.hubilon.modules.scheduler;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.scheduler.application.service.WeeklyReportProcessor;
import com.hubilon.modules.scheduler.application.service.strategy.AiUploadStrategy;
import com.hubilon.modules.scheduler.application.service.strategy.ManualUploadStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportProcessorTest {

    @Mock
    FolderSummaryQueryPort folderSummaryQueryPort;

    @Mock
    ManualUploadStrategy manualUploadStrategy;

    @Mock
    AiUploadStrategy aiUploadStrategy;

    @InjectMocks
    WeeklyReportProcessor weeklyReportProcessor;

    private static final LocalDate START = LocalDate.of(2026, 4, 14);
    private static final LocalDate END = LocalDate.of(2026, 4, 20);

    private FolderResult sampleFolder() {
        return new FolderResult(1L, "개발팀 폴더", 1L, "개발", FolderStatus.IN_PROGRESS, 0, 1L, null, null);
    }

    private WeeklyReportRowDto sampleRow(String progressSummary, String planSummary) {
        return new WeeklyReportRowDto(1L, "개발", "개발팀 폴더", List.of(), progressSummary, planSummary);
    }

    @Test
    void 수동_내용이_있으면_ManualUploadStrategy_실행() {
        FolderSummary summary = FolderSummary.builder()
                .id(10L)
                .folderId(1L)
                .startDate(START)
                .endDate(END)
                .progressSummary("이번 주 진행 내용")
                .planSummary("다음 주 계획")
                .build();

        WeeklyReportRowDto expectedRow = sampleRow("이번 주 진행 내용", "다음 주 계획");

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(manualUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn(expectedRow);

        WeeklyReportRowDto row = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(row).isNotNull();
        assertThat(row.progressSummary()).isEqualTo("이번 주 진행 내용");
        assertThat(row.planSummary()).isEqualTo("다음 주 계획");
        verify(manualUploadStrategy).execute(any(), eq(summary), eq(START), eq(END));
        verify(aiUploadStrategy, never()).execute(any(), any(), any(), any());
    }

    @Test
    void progressSummary가_비어있으면_AiUploadStrategy_실행() {
        FolderSummary summary = FolderSummary.builder()
                .id(10L)
                .folderId(1L)
                .startDate(START)
                .endDate(END)
                .progressSummary("")
                .planSummary("다음 주 계획")
                .build();

        WeeklyReportRowDto expectedRow = sampleRow("AI 생성 진행 내용", "다음 주 계획");

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(aiUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn(expectedRow);

        WeeklyReportRowDto row = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(row).isNotNull();
        assertThat(row.progressSummary()).isEqualTo("AI 생성 진행 내용");
        verify(aiUploadStrategy).execute(any(), eq(summary), eq(START), eq(END));
        verify(manualUploadStrategy, never()).execute(any(), any(), any(), any());
    }

    @Test
    void planSummary가_비어있으면_AiUploadStrategy_실행() {
        FolderSummary summary = FolderSummary.builder()
                .id(10L)
                .folderId(1L)
                .startDate(START)
                .endDate(END)
                .progressSummary("이번 주 진행 내용")
                .planSummary(null)
                .build();

        WeeklyReportRowDto expectedRow = sampleRow("이번 주 진행 내용", "AI 생성 계획");

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(aiUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn(expectedRow);

        WeeklyReportRowDto row = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(row).isNotNull();
        assertThat(row.planSummary()).isEqualTo("AI 생성 계획");
        verify(aiUploadStrategy).execute(any(), eq(summary), eq(START), eq(END));
        verify(manualUploadStrategy, never()).execute(any(), any(), any(), any());
    }

    @Test
    void FolderSummary가_없으면_AiUploadStrategy_실행_null_전달() {
        WeeklyReportRowDto expectedRow = sampleRow("AI 생성 진행 내용", "AI 생성 계획");

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.empty());
        when(aiUploadStrategy.execute(any(), eq(null), eq(START), eq(END)))
                .thenReturn(expectedRow);

        WeeklyReportRowDto row = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(row).isNotNull();
        verify(aiUploadStrategy).execute(any(), eq(null), eq(START), eq(END));
        verify(manualUploadStrategy, never()).execute(any(), any(), any(), any());
    }
}
