package com.hubilon.modules.folder.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "work_projects")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WorkProjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("소속 폴더")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private FolderJpaEntity folder;

    @Comment("세부 프로젝트명")
    @Column(nullable = false)
    private String name;

    @Comment("정렬 순서")
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int sortOrder;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public WorkProjectJpaEntity(Long id, FolderJpaEntity folder, String name, int sortOrder) {
        this.id = id;
        this.folder = folder;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void updateFolder(FolderJpaEntity folder) {
        this.folder = folder;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
