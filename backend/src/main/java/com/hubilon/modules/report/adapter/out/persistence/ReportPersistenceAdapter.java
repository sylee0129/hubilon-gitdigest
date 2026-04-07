package com.hubilon.modules.report.adapter.out.persistence;

import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FileChange;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReportPersistenceAdapter implements ReportCommandPort, ReportQueryPort {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public Report save(Report report) {
        ReportJpaEntity entity;
        if (report.getId() != null) {
            entity = reportJpaRepository.findById(report.getId())
                    .orElse(buildEntity(report));
            if (report.isManuallyEdited()) {
                entity.updateSummary(report.getManualSummary());
            } else {
                entity.refreshAiSummary(report.getAiSummary());
                if (report.getCommits() != null) {
                    entity.clearCommits();
                    populateCommits(entity, report);
                }
            }
        } else {
            entity = buildEntity(report);
            populateCommits(entity, report);
        }
        ReportJpaEntity saved = reportJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Report> findById(Long id) {
        return reportJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Report> findByProjectIdAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        return reportJpaRepository.findByProjectIdAndDateRange(projectId, startDate, endDate)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Report> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return reportJpaRepository.findByDateRange(startDate, endDate)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Report> findExisting(Long projectId, LocalDate startDate, LocalDate endDate) {
        return reportJpaRepository.findExisting(projectId, startDate, endDate).map(this::toDomain);
    }

    private ReportJpaEntity buildEntity(Report report) {
        return ReportJpaEntity.builder()
                .id(report.getId())
                .projectId(report.getProjectId())
                .projectName(report.getProjectName())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .aiSummary(report.getAiSummary())
                .manualSummary(report.getManualSummary())
                .manuallyEdited(report.isManuallyEdited())
                .build();
    }

    private void populateCommits(ReportJpaEntity entity, Report report) {
        if (report.getCommits() == null) return;
        for (CommitInfo commit : report.getCommits()) {
            CommitInfoJpaEntity commitEntity = CommitInfoJpaEntity.builder()
                    .sha(commit.getSha())
                    .authorName(commit.getAuthorName())
                    .authorEmail(commit.getAuthorEmail())
                    .committedAt(commit.getCommittedAt())
                    .message(commit.getMessage())
                    .build();

            if (commit.getFileChanges() != null) {
                for (FileChange fc : commit.getFileChanges()) {
                    FileChangeJpaEntity fcEntity = FileChangeJpaEntity.builder()
                            .oldPath(fc.getOldPath())
                            .newPath(fc.getNewPath())
                            .newFile(fc.isNewFile())
                            .renamedFile(fc.isRenamedFile())
                            .deletedFile(fc.isDeletedFile())
                            .addedLines(fc.getAddedLines())
                            .removedLines(fc.getRemovedLines())
                            .build();
                    commitEntity.addFileChange(fcEntity);
                }
            }
            entity.addCommit(commitEntity);
        }
    }

    private Report toDomain(ReportJpaEntity entity) {
        List<CommitInfo> commits = entity.getCommits() == null
                ? Collections.emptyList()
                : entity.getCommits().stream().map(this::toCommitDomain).toList();

        return Report.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .projectName(entity.getProjectName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .aiSummary(entity.getAiSummary())
                .manualSummary(entity.getManualSummary())
                .manuallyEdited(entity.isManuallyEdited())
                .commits(commits)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CommitInfo toCommitDomain(CommitInfoJpaEntity entity) {
        List<FileChange> fileChanges = entity.getFileChanges() == null
                ? Collections.emptyList()
                : entity.getFileChanges().stream().map(this::toFileChangeDomain).toList();

        return CommitInfo.builder()
                .id(entity.getId())
                .sha(entity.getSha())
                .authorName(entity.getAuthorName())
                .authorEmail(entity.getAuthorEmail())
                .committedAt(entity.getCommittedAt())
                .message(entity.getMessage())
                .fileChanges(fileChanges)
                .build();
    }

    private FileChange toFileChangeDomain(FileChangeJpaEntity entity) {
        return FileChange.builder()
                .oldPath(entity.getOldPath())
                .newPath(entity.getNewPath())
                .newFile(entity.isNewFile())
                .renamedFile(entity.isRenamedFile())
                .deletedFile(entity.isDeletedFile())
                .addedLines(entity.getAddedLines())
                .removedLines(entity.getRemovedLines())
                .build();
    }
}
