package com.hubilon.modules.dashboard;

import com.hubilon.modules.dashboard.application.dto.DashboardSummaryResult;
import com.hubilon.modules.dashboard.application.port.out.DashboardQueryPort;
import com.hubilon.modules.dashboard.application.service.DashboardQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceTest {

    @Mock
    DashboardQueryPort dashboardQueryPort;

    @InjectMocks
    DashboardQueryService dashboardQueryService;

    @Test
    void 대시보드_요약_정상_반환() {
        DashboardSummaryResult expected = new DashboardSummaryResult(
                5,
                3,
                10,
                42,
                List.of(
                        new DashboardSummaryResult.ActiveFolderItem(1L, "개발팀", 7, LocalDateTime.of(2026, 4, 9, 12, 0)),
                        new DashboardSummaryResult.ActiveFolderItem(2L, "디자인팀", 3, LocalDateTime.of(2026, 4, 9, 10, 0))
                )
        );

        when(dashboardQueryPort.query()).thenReturn(expected);

        DashboardSummaryResult actual = dashboardQueryService.getSummary();

        assertThat(actual.totalFolderCount()).isEqualTo(5);
        assertThat(actual.inProgressFolderCount()).isEqualTo(3);
        assertThat(actual.todayCommitCount()).isEqualTo(10);
        assertThat(actual.weeklyCommitCount()).isEqualTo(42);
        assertThat(actual.recentActiveFolders()).hasSize(2);
        assertThat(actual.recentActiveFolders().get(0).folderName()).isEqualTo("개발팀");
        assertThat(actual.recentActiveFolders().get(0).commitCount()).isEqualTo(7);

        verify(dashboardQueryPort, times(1)).query();
    }

    @Test
    void 대시보드_요약_활성폴더_없을때() {
        DashboardSummaryResult expected = new DashboardSummaryResult(2, 0, 0, 0, List.of());

        when(dashboardQueryPort.query()).thenReturn(expected);

        DashboardSummaryResult actual = dashboardQueryService.getSummary();

        assertThat(actual.totalFolderCount()).isEqualTo(2);
        assertThat(actual.inProgressFolderCount()).isEqualTo(0);
        assertThat(actual.todayCommitCount()).isEqualTo(0);
        assertThat(actual.weeklyCommitCount()).isEqualTo(0);
        assertThat(actual.recentActiveFolders()).isEmpty();

        verify(dashboardQueryPort, times(1)).query();
    }

    @Test
    void 대시보드_요약_recentActiveFolders_최대3개() {
        List<DashboardSummaryResult.ActiveFolderItem> items = List.of(
                new DashboardSummaryResult.ActiveFolderItem(1L, "폴더A", 5, LocalDateTime.now()),
                new DashboardSummaryResult.ActiveFolderItem(2L, "폴더B", 3, LocalDateTime.now()),
                new DashboardSummaryResult.ActiveFolderItem(3L, "폴더C", 1, LocalDateTime.now())
        );
        DashboardSummaryResult expected = new DashboardSummaryResult(10, 3, 5, 20, items);

        when(dashboardQueryPort.query()).thenReturn(expected);

        DashboardSummaryResult actual = dashboardQueryService.getSummary();

        assertThat(actual.recentActiveFolders()).hasSize(3);
        assertThat(actual.recentActiveFolders().get(0).folderId()).isEqualTo(1L);
        assertThat(actual.recentActiveFolders().get(2).folderId()).isEqualTo(3L);
    }
}
