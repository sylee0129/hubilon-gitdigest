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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FolderSummary withAiSummary(String aiSummary, boolean failed) {
        return FolderSummary.builder()
                .id(this.id)
                .folderId(this.folderId)
                .folderName(this.folderName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .totalCommitCount(this.totalCommitCount)
                .uniqueContributorCount(this.uniqueContributorCount)
                .summary(aiSummary)
                .manuallyEdited(false)
                .aiSummaryFailed(failed)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public FolderSummary withManualSummary(String manualSummary) {
        return FolderSummary.builder()
                .id(this.id)
                .folderId(this.folderId)
                .folderName(this.folderName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .totalCommitCount(this.totalCommitCount)
                .uniqueContributorCount(this.uniqueContributorCount)
                .summary(manualSummary)
                .manuallyEdited(true)
                .aiSummaryFailed(this.aiSummaryFailed)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
