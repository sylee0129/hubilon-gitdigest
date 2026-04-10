package com.hubilon.modules.dashboard.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hubilon.modules.report.adapter.out.persistence.CommitInfoJpaEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardCommitQueryRepository extends JpaRepository<CommitInfoJpaEntity, Long> {

    @Query("SELECT COUNT(c) FROM CommitInfoJpaEntity c WHERE c.committedAt >= :start AND c.committedAt < :end")
    int countByCommittedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT new com.hubilon.modules.dashboard.adapter.out.persistence.ActiveFolderProjection(
                p.folderId,
                f.name,
                COUNT(c.id),
                MAX(c.committedAt)
            )
            FROM CommitInfoJpaEntity c
            JOIN c.report r
            JOIN com.hubilon.modules.project.adapter.out.persistence.ProjectJpaEntity p ON p.id = r.projectId
            JOIN com.hubilon.modules.folder.adapter.out.persistence.FolderJpaEntity f ON f.id = p.folderId
            WHERE c.committedAt >= :since
              AND p.folderId IS NOT NULL
            GROUP BY p.folderId, f.name
            ORDER BY COUNT(c.id) DESC
            """)
    List<ActiveFolderProjection> findTopActiveFoldersSince(@Param("since") LocalDateTime since);
}
