package com.hubilon.modules.report.application.mapper;

import com.hubilon.modules.report.application.dto.CommitInfoResult;
import com.hubilon.modules.report.application.dto.FileChangeResult;
import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FileChange;
import com.hubilon.modules.report.domain.model.Report;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ReportAppMapper {

    public ReportResult toResult(Report report) {
        return toResult(report, true);
    }

    public ReportResult toResult(Report report, boolean aiUsed) {
        List<CommitInfoResult> commitResults = report.getCommits() == null
                ? Collections.emptyList()
                : report.getCommits().stream().map(this::toCommitResult).toList();

        int commitCount = commitResults.size();
        int contributorCount = (int) commitResults.stream()
                .map(CommitInfoResult::authorEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .count();

        return new ReportResult(
                report.getId(),
                report.getProjectId(),
                report.getProjectName(),
                report.getStartDate(),
                report.getEndDate(),
                report.getEffectiveSummary(),
                report.isManuallyEdited(),
                commitResults,
                commitCount,
                contributorCount,
                report.getCreatedAt(),
                !aiUsed
        );
    }

    public CommitInfoResult toCommitResult(CommitInfo commit) {
        List<FileChangeResult> fileResults = commit.getFileChanges() == null
                ? Collections.emptyList()
                : commit.getFileChanges().stream().map(this::toFileChangeResult).toList();

        return new CommitInfoResult(
                commit.getId(),
                commit.getSha(),
                commit.getAuthorName(),
                commit.getAuthorEmail(),
                commit.getCommittedAt(),
                commit.getMessage(),
                fileResults
        );
    }

    public FileChangeResult toFileChangeResult(FileChange fileChange) {
        return new FileChangeResult(
                fileChange.getOldPath(),
                fileChange.getNewPath(),
                fileChange.isNewFile(),
                fileChange.isRenamedFile(),
                fileChange.isDeletedFile(),
                fileChange.getAddedLines(),
                fileChange.getRemovedLines()
        );
    }
}
