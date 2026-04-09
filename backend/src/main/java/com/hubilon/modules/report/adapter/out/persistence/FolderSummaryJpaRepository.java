package com.hubilon.modules.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FolderSummaryJpaRepository extends JpaRepository<FolderSummaryJpaEntity, Long> {

    @Query("SELECT fs FROM FolderSummaryJpaEntity fs WHERE fs.folderId = :folderId " +
           "AND fs.startDate = :startDate AND fs.endDate = :endDate")
    Optional<FolderSummaryJpaEntity> findByFolderIdAndDateRange(
            @Param("folderId") Long folderId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
