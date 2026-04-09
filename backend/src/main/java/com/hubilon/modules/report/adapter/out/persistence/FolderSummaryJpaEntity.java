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

@Getter
@Entity
@Table(
        name = "folder_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_folder_summary_folder_date",
                columnNames = {"folder_id", "start_date", "end_date"}
        )
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FolderSummaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("폴더 ID (FK)")
    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Comment("폴더 이름 (비정규화 — 조회 편의)")
    @Column(name = "folder_name", nullable = false)
    private String folderName;

    @Comment("보고 시작일")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Comment("보고 종료일")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Comment("총 커밋 수")
    @Column(name = "total_commit_count", nullable = false)
    private int totalCommitCount;

    @Comment("고유 기여자 수")
    @Column(name = "unique_contributor_count", nullable = false)
    private int uniqueContributorCount;

    @Comment("요약 내용")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Comment("수동 편집 여부")
    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;

    @Comment("AI 요약 실패 여부")
    @Column(name = "ai_summary_failed", nullable = false)
    private boolean aiSummaryFailed;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public FolderSummaryJpaEntity(Long id, Long folderId, String folderName,
                                   LocalDate startDate, LocalDate endDate,
                                   int totalCommitCount, int uniqueContributorCount,
                                   String summary, boolean manuallyEdited, boolean aiSummaryFailed) {
        this.id = id;
        this.folderId = folderId;
        this.folderName = folderName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCommitCount = totalCommitCount;
        this.uniqueContributorCount = uniqueContributorCount;
        this.summary = summary;
        this.manuallyEdited = manuallyEdited;
        this.aiSummaryFailed = aiSummaryFailed;
    }

    public void updateSummary(String summary, boolean manuallyEdited, boolean aiSummaryFailed,
                              int totalCommitCount, int uniqueContributorCount) {
        this.summary = summary;
        this.manuallyEdited = manuallyEdited;
        this.aiSummaryFailed = aiSummaryFailed;
        this.totalCommitCount = totalCommitCount;
        this.uniqueContributorCount = uniqueContributorCount;
    }
}
