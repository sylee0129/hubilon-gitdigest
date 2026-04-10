package com.hubilon.modules.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class FolderSummary {

    private Long id;
    private Long folderId;
    private String folderName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalCommitCount;
    private int uniqueContributorCount;
    private String summary;
    private boolean manuallyEdited;
    private boolean aiSummaryFailed;
    private String progressSummary;
    private String planSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FolderSummary withAiSummary(String progressSummary, String planSummary, boolean failed) {
        return FolderSummary.builder()
                .id(this.id)
                .folderId(this.folderId)
                .folderName(this.folderName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .totalCommitCount(this.totalCommitCount)
                .uniqueContributorCount(this.uniqueContributorCount)
                .summary(progressSummary)
                .manuallyEdited(false)
                .aiSummaryFailed(failed)
                .progressSummary(progressSummary)
                .planSummary(planSummary)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public FolderSummary withManualSummary(String progressSummary, String planSummary) {
        return FolderSummary.builder()
                .id(this.id)
                .folderId(this.folderId)
                .folderName(this.folderName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .totalCommitCount(this.totalCommitCount)
                .uniqueContributorCount(this.uniqueContributorCount)
                .summary(progressSummary)
                .manuallyEdited(true)
                .aiSummaryFailed(this.aiSummaryFailed)
                .progressSummary(progressSummary)
                .planSummary(planSummary)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
