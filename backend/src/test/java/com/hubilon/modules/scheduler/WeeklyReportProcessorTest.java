package com.hubilon.modules.scheduler;

import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderCategory;
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
        return new FolderResult(1L, "개발팀 폴더", FolderCategory.DEVELOPMENT, FolderStatus.IN_PROGRESS, 0, null, null);
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

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(manualUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn("https://confluence.example.com/page/1");

        String url = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(url).isEqualTo("https://confluence.example.com/page/1");
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
                .progressSummary("") // 비어있음
                .planSummary("다음 주 계획")
                .build();

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(aiUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn("https://confluence.example.com/ai/1");

        String url = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(url).isEqualTo("https://confluence.example.com/ai/1");
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
                .planSummary(null) // null
                .build();

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.of(summary));
        when(aiUploadStrategy.execute(any(), eq(summary), eq(START), eq(END)))
                .thenReturn("https://confluence.example.com/ai/2");

        String url = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(url).isEqualTo("https://confluence.example.com/ai/2");
        verify(aiUploadStrategy).execute(any(), eq(summary), eq(START), eq(END));
        verify(manualUploadStrategy, never()).execute(any(), any(), any(), any());
    }

    @Test
    void FolderSummary가_없으면_AiUploadStrategy_실행_null_전달() {
        when(folderSummaryQueryPort.findByFolderIdAndDateRange(1L, START, END))
                .thenReturn(Optional.empty());
        when(aiUploadStrategy.execute(any(), eq(null), eq(START), eq(END)))
                .thenReturn("https://confluence.example.com/ai/new");

        String url = weeklyReportProcessor.process(sampleFolder(), START, END);

        assertThat(url).isEqualTo("https://confluence.example.com/ai/new");
        verify(aiUploadStrategy).execute(any(), eq(null), eq(START), eq(END));
        verify(manualUploadStrategy, never()).execute(any(), any(), any(), any());
    }
}
