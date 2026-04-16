package com.hubilon.modules.scheduler.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "scheduler_folder_results")
@NoArgsConstructor(access = PROTECTED)
public class SchedulerFolderResultJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_log_id", nullable = false)
    private SchedulerJobLogJpaEntity jobLog;

    @Comment("폴더 ID")
    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Comment("폴더 이름")
    @Column(name = "folder_name", nullable = false)
    private String folderName;

    @Comment("처리 성공 여부")
    @Column(name = "success", nullable = false)
    private boolean success;

    @Comment("에러 메시지 (실패 시)")
    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @Comment("Confluence 페이지 URL")
    @Column(name = "confluence_page_url", length = 500)
    private String confluencePageUrl;

    @Builder
    public SchedulerFolderResultJpaEntity(Long folderId, String folderName,
                                           boolean success, String errorMessage,
                                           String confluencePageUrl) {
        this.folderId = folderId;
        this.folderName = folderName;
        this.success = success;
        this.errorMessage = errorMessage;
        this.confluencePageUrl = confluencePageUrl;
    }

    void assignJobLog(SchedulerJobLogJpaEntity jobLog) {
        this.jobLog = jobLog;
    }
}
