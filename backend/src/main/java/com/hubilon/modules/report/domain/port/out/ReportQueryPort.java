package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.Report;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportQueryPort {

    Optional<Report> findById(Long id);

    List<Report> findByProjectIdAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate);

    List<Report> findByDateRange(LocalDate startDate, LocalDate endDate);

    Optional<Report> findExisting(Long projectId, LocalDate startDate, LocalDate endDate);
}
