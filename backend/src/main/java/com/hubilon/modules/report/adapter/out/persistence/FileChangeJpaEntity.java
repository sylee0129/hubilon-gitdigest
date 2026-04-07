package com.hubilon.modules.report.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "file_changes")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class FileChangeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_info_id", nullable = false)
    private CommitInfoJpaEntity commitInfo;

    @Comment("이전 파일 경로")
    private String oldPath;

    @Comment("새 파일 경로")
    private String newPath;

    @Comment("신규 파일 여부")
    private boolean newFile;

    @Comment("이름 변경 여부")
    private boolean renamedFile;

    @Comment("삭제 여부")
    private boolean deletedFile;

    @Comment("추가된 라인 수")
    private int addedLines;

    @Comment("삭제된 라인 수")
    private int removedLines;

    @Builder
    public FileChangeJpaEntity(String oldPath, String newPath, boolean newFile,
                                boolean renamedFile, boolean deletedFile,
                                int addedLines, int removedLines) {
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.newFile = newFile;
        this.renamedFile = renamedFile;
        this.deletedFile = deletedFile;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
    }

    public void assignCommit(CommitInfoJpaEntity commitInfo) {
        this.commitInfo = commitInfo;
    }
}
