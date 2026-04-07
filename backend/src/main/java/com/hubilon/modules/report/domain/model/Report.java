package com.hubilon.modules.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Report {

    private Long id;
    private Long projectId;
    private String projectName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String aiSummary;
    private String manualSummary;
    private boolean manuallyEdited;
    private List<CommitInfo> commits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getEffectiveSummary() {
        return manuallyEdited && manualSummary != null ? manualSummary : aiSummary;
    }

    public Report withAiSummary(String summary) {
        return Report.builder()
                .id(this.id)
                .projectId(this.projectId)
                .projectName(this.projectName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .aiSummary(summary)
                .manualSummary(this.manualSummary)
                .manuallyEdited(this.manuallyEdited)
                .commits(null) // DB에 이미 저장된 커밋은 재저장하지 않음
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }

    public Report withManualSummary(String summary) {
        return Report.builder()
                .id(this.id)
                .projectId(this.projectId)
                .projectName(this.projectName)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .aiSummary(this.aiSummary)
                .manualSummary(summary)
                .manuallyEdited(true)
                .commits(this.commits)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
