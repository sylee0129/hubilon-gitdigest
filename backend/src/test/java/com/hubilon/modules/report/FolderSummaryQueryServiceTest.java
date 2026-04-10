package com.hubilon.modules.report;

import com.hubilon.modules.report.application.dto.FolderSummaryQuery;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.application.service.FolderSummaryQueryService;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderSummaryQueryServiceTest {

    @Mock
    FolderSummaryQueryPort folderSummaryQueryPort;

    @Mock
    FolderSummaryAppMapper folderSummaryAppMapper;

    @InjectMocks
    FolderSummaryQueryService folderSummaryQueryService;

    @Test
    void 폴더요약_존재하면_결과_반환() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 7);

        FolderSummary domain = FolderSummary.builder()
                .id(1L)
                .folderId(10L)
                .folderName("개발팀 폴더")
                .startDate(start)
                .endDate(end)
                .totalCommitCount(5)
                .uniqueContributorCount(2)
                .summary("주간 커밋 요약")
                .manuallyEdited(false)
                .aiSummaryFailed(false)
                .build();

        FolderSummaryResult result = new FolderSummaryResult(
                1L, 10L, "개발팀 폴더", start, end, 5, 2, "주간 커밋 요약", false, false, null, null, null, null);

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(10L, start, end))
                .thenReturn(Optional.of(domain));
        when(folderSummaryAppMapper.toResult(domain)).thenReturn(result);

        FolderSummaryQuery query = new FolderSummaryQuery(10L, start, end);
        Optional<FolderSummaryResult> actual = folderSummaryQueryService.query(query);

        assertThat(actual).isPresent();
        assertThat(actual.get().folderId()).isEqualTo(10L);
        assertThat(actual.get().totalCommitCount()).isEqualTo(5);

        verify(folderSummaryQueryPort).findByFolderIdAndDateRange(10L, start, end);
        verify(folderSummaryAppMapper).toResult(domain);
    }

    @Test
    void 폴더요약_없으면_빈_Optional_반환() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 7);

        when(folderSummaryQueryPort.findByFolderIdAndDateRange(99L, start, end))
                .thenReturn(Optional.empty());

        FolderSummaryQuery query = new FolderSummaryQuery(99L, start, end);
        Optional<FolderSummaryResult> actual = folderSummaryQueryService.query(query);

        assertThat(actual).isEmpty();
        verify(folderSummaryAppMapper, never()).toResult(any());
    }
}
