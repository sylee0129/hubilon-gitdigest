package com.hubilon.modules.report.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "commit_infos")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CommitInfoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReportJpaEntity report;

    @Comment("커밋 SHA")
    @Column(nullable = false)
    private String sha;

    @Comment("작성자 이름")
    private String authorName;

    @Comment("작성자 이메일")
    private String authorEmail;

    @Comment("커밋 시각")
    private LocalDateTime committedAt;

    @Comment("커밋 메시지")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToMany(mappedBy = "commitInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileChangeJpaEntity> fileChanges = new ArrayList<>();

    @Builder
    public CommitInfoJpaEntity(String sha, String authorName, String authorEmail,
                                LocalDateTime committedAt, String message) {
        this.sha = sha;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.committedAt = committedAt;
        this.message = message;
    }

    public void assignReport(ReportJpaEntity report) {
        this.report = report;
    }

    public void addFileChange(FileChangeJpaEntity fileChange) {
        this.fileChanges.add(fileChange);
        fileChange.assignCommit(this);
    }
}
