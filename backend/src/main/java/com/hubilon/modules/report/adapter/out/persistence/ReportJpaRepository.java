package com.hubilon.modules.report.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    @Query("SELECT r FROM ReportJpaEntity r WHERE r.projectId = :projectId " +
           "AND r.startDate >= :startDate AND r.endDate <= :endDate")
    List<ReportJpaEntity> findByProjectIdAndDateRange(
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM ReportJpaEntity r WHERE r.projectId IN :projectIds " +
           "AND r.startDate >= :startDate AND r.endDate <= :endDate")
    List<ReportJpaEntity> findByProjectIdsAndDateRange(
            @Param("projectIds") List<Long> projectIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM ReportJpaEntity r WHERE r.startDate >= :startDate AND r.endDate <= :endDate")
    List<ReportJpaEntity> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM ReportJpaEntity r WHERE r.projectId = :projectId " +
           "AND r.startDate = :startDate AND r.endDate = :endDate")
    Optional<ReportJpaEntity> findExisting(
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
