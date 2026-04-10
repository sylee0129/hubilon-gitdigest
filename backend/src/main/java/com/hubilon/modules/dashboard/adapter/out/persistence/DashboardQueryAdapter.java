package com.hubilon.modules.dashboard.adapter.out.persistence;

import com.hubilon.modules.dashboard.application.dto.DashboardSummaryResult;
import com.hubilon.modules.dashboard.application.port.out.DashboardQueryPort;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardQueryAdapter implements DashboardQueryPort {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int RECENT_ACTIVE_FOLDER_LIMIT = 3;

    private final FolderJpaRepository folderJpaRepository;
    private final DashboardCommitQueryRepository dashboardCommitQueryRepository;

    @Override
    public DashboardSummaryResult query() {
        int totalFolderCount = (int) folderJpaRepository.count();
        int inProgressFolderCount = folderJpaRepository.findAllWithDetailsByStatus(FolderStatus.IN_PROGRESS).size();

        ZonedDateTime nowKst = ZonedDateTime.now(KST);

        LocalDateTime todayStart = nowKst.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        int todayCommitCount = dashboardCommitQueryRepository.countByCommittedAtBetween(todayStart, todayEnd);

        LocalDateTime weekStart = nowKst.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusDays(7);
        int weeklyCommitCount = dashboardCommitQueryRepository.countByCommittedAtBetween(weekStart, weekEnd);

        LocalDateTime since24h = nowKst.minusHours(24).toLocalDateTime();
        List<ActiveFolderProjection> projections = dashboardCommitQueryRepository
                .findTopActiveFoldersSince(since24h);

        List<DashboardSummaryResult.ActiveFolderItem> recentActiveFolders = projections.stream()
                .limit(RECENT_ACTIVE_FOLDER_LIMIT)
                .map(p -> new DashboardSummaryResult.ActiveFolderItem(
                        p.folderId(),
                        p.folderName(),
                        (int) p.commitCount(),
                        p.lastCommittedAt()
                ))
                .toList();

        return new DashboardSummaryResult(
                totalFolderCount,
                inProgressFolderCount,
                todayCommitCount,
                weeklyCommitCount,
                recentActiveFolders
        );
    }
}
