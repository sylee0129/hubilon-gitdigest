package com.hubilon.modules.scheduler.adapter.out.persistence;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "scheduler_job_logs")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SchedulerJobLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("실행 시각")
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Comment("잡 상태")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SchedulerJobStatus status;

    @Comment("전체 폴더 수")
    @Column(name = "total_folder_count", nullable = false)
    private int totalFolderCount;

    @Comment("성공 폴더 수")
    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Comment("실패 폴더 수")
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "jobLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SchedulerFolderResultJpaEntity> folderResults = new ArrayList<>();

    @Builder
    public SchedulerJobLogJpaEntity(Long id, LocalDateTime executedAt, SchedulerJobStatus status,
                                     int totalFolderCount, int successCount, int failCount) {
        this.id = id;
        this.executedAt = executedAt;
        this.status = status;
        this.totalFolderCount = totalFolderCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.folderResults = new ArrayList<>();
    }

    public void updateStatus(SchedulerJobStatus status, int successCount, int failCount) {
        this.status = status;
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public void addFolderResult(SchedulerFolderResultJpaEntity result) {
        result.assignJobLog(this);
        this.folderResults.add(result);
    }
}
