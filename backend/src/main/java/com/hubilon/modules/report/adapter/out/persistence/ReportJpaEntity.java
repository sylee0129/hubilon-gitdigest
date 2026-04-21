package com.hubilon.modules.report.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("프로젝트 ID (FK)")
    @Column(nullable = false)
    private Long projectId;

    @Comment("프로젝트 이름 (비정규화 — 조회 편의)")
    @Column(nullable = false)
    private String projectName;

    @Comment("보고 시작일")
    @Column(nullable = false)
    private LocalDate startDate;

    @Comment("보고 종료일")
    @Column(nullable = false)
    private LocalDate endDate;

    @Comment("AI 생성 요약")
    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Comment("수동 편집 요약")
    @Column(columnDefinition = "TEXT")
    private String manualSummary;

    @Comment("수동 편집 여부")
    @Column(nullable = false)
    private boolean manuallyEdited;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommitInfoJpaEntity> commits = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public ReportJpaEntity(Long id, Long projectId, String projectName, LocalDate startDate,
                            LocalDate endDate, String aiSummary, String manualSummary, boolean manuallyEdited) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.aiSummary = aiSummary;
        this.manualSummary = manualSummary;
        this.manuallyEdited = manuallyEdited;
    }

    public void updateSummary(String manualSummary) {
        this.manualSummary = manualSummary;
        this.manuallyEdited = true;
    }

    public void refreshAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
        this.manuallyEdited = false;
    }

    public void clearCommits() {
        this.commits.clear();
    }

    public void addCommit(CommitInfoJpaEntity commit) {
        this.commits.add(commit);
        commit.assignReport(this);
    }
}
