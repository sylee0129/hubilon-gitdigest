package com.hubilon.modules.report;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.dto.FolderSummaryUpdateCommand;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.application.service.FolderSummaryUpdateService;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderSummaryUpdateServiceTest {

    @Mock
    FolderSummaryQueryPort folderSummaryQueryPort;

    @Mock
    FolderSummaryCommandPort folderSummaryCommandPort;

    @Mock
    FolderSummaryAppMapper folderSummaryAppMapper;

    @InjectMocks
    FolderSummaryUpdateService service;

    private static final Long SUMMARY_ID = 1L;
    private static final Long FOLDER_ID = 10L;

    private FolderSummary existingSummary;

    @BeforeEach
    void setUp() {
        existingSummary = FolderSummary.builder()
                .id(SUMMARY_ID)
                .folderId(FOLDER_ID)
                .folderName("개발팀")
                .startDate(LocalDate.of(2026, 4, 7))
                .endDate(LocalDate.of(2026, 4, 11))
                .totalCommitCount(5)
                .uniqueContributorCount(2)
                .summary("기존 요약")
                .progressSummary("기존 진행사항")
                .planSummary("기존 계획")
                .manuallyEdited(false)
                .aiSummaryFailed(false)
                .build();
    }

    @Test
    void progressSummary가_null이면_기존_값_보존() {
        when(folderSummaryQueryPort.findById(SUMMARY_ID)).thenReturn(Optional.of(existingSummary));

        FolderSummaryUpdateCommand command = new FolderSummaryUpdateCommand(null, null, "새 계획");

        FolderSummary captured = existingSummary.withManualSummary("기존 진행사항", "새 계획");
        FolderSummaryResult expectedResult = new FolderSummaryResult(
                SUMMARY_ID, FOLDER_ID, "개발팀",
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 11),
                5, 2,
                "기존 진행사항", true, false,
                "기존 진행사항", "새 계획",
                null, null
        );

        when(folderSummaryCommandPort.save(any())).thenReturn(captured);
        when(folderSummaryAppMapper.toResult(captured)).thenReturn(expectedResult);

        FolderSummaryResult result = service.update(SUMMARY_ID, command);

        assertThat(result.progressSummary()).isEqualTo("기존 진행사항");
        assertThat(result.planSummary()).isEqualTo("새 계획");

        ArgumentCaptor<FolderSummary> captor = ArgumentCaptor.forClass(FolderSummary.class);
        verify(folderSummaryCommandPort).save(captor.capture());
        assertThat(captor.getValue().getProgressSummary()).isEqualTo("기존 진행사항");
    }

    @Test
    void progressSummary가_빈_문자열이면_null로_처리() {
        when(folderSummaryQueryPort.findById(SUMMARY_ID)).thenReturn(Optional.of(existingSummary));

        FolderSummaryUpdateCommand command = new FolderSummaryUpdateCommand(null, "", "새 계획");

        FolderSummary captured = existingSummary.withManualSummary(null, "새 계획");
        FolderSummaryResult expectedResult = new FolderSummaryResult(
                SUMMARY_ID, FOLDER_ID, "개발팀",
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 11),
                5, 2,
                null, true, false,
                null, "새 계획",
                null, null
        );

        when(folderSummaryCommandPort.save(any())).thenReturn(captured);
        when(folderSummaryAppMapper.toResult(captured)).thenReturn(expectedResult);

        FolderSummaryResult result = service.update(SUMMARY_ID, command);

        assertThat(result.progressSummary()).isNull();

        ArgumentCaptor<FolderSummary> captor = ArgumentCaptor.forClass(FolderSummary.class);
        verify(folderSummaryCommandPort).save(captor.capture());
        assertThat(captor.getValue().getProgressSummary()).isNull();
    }

    @Test
    void progressSummary에_값이_있으면_신규_값_사용() {
        when(folderSummaryQueryPort.findById(SUMMARY_ID)).thenReturn(Optional.of(existingSummary));

        FolderSummaryUpdateCommand command = new FolderSummaryUpdateCommand(null, "새 진행사항", "새 계획");

        FolderSummary captured = existingSummary.withManualSummary("새 진행사항", "새 계획");
        FolderSummaryResult expectedResult = new FolderSummaryResult(
                SUMMARY_ID, FOLDER_ID, "개발팀",
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 11),
                5, 2,
                "새 진행사항", true, false,
                "새 진행사항", "새 계획",
                null, null
        );

        when(folderSummaryCommandPort.save(any())).thenReturn(captured);
        when(folderSummaryAppMapper.toResult(captured)).thenReturn(expectedResult);

        FolderSummaryResult result = service.update(SUMMARY_ID, command);

        assertThat(result.progressSummary()).isEqualTo("새 진행사항");
        assertThat(result.planSummary()).isEqualTo("새 계획");

        ArgumentCaptor<FolderSummary> captor = ArgumentCaptor.forClass(FolderSummary.class);
        verify(folderSummaryCommandPort).save(captor.capture());
        assertThat(captor.getValue().getProgressSummary()).isEqualTo("새 진행사항");
        assertThat(captor.getValue().isManuallyEdited()).isTrue();
    }

    @Test
    void 존재하지않는_요약id이면_NotFoundException() {
        when(folderSummaryQueryPort.findById(999L)).thenReturn(Optional.empty());

        FolderSummaryUpdateCommand command = new FolderSummaryUpdateCommand(null, "내용", "계획");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.update(999L, command)
        ).isInstanceOf(NotFoundException.class);
    }
}
