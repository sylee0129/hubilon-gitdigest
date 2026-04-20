package com.hubilon.modules.folder.adapter.out.persistence;

import com.hubilon.modules.category.adapter.out.persistence.CategoryJpaEntity;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "folders")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FolderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("프로젝트명")
    @Column(nullable = false)
    private String name;

    @Comment("카테고리")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @Comment("상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FolderStatus status;

    @Comment("정렬 순서")
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Comment("소속 팀")
    private TeamJpaEntity team;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<FolderMemberJpaEntity> members = new HashSet<>();

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WorkProjectJpaEntity> workProjects = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public FolderJpaEntity(Long id, String name, CategoryJpaEntity category, FolderStatus status, int sortOrder, TeamJpaEntity team) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.status = status;
        this.sortOrder = sortOrder;
        this.team = team;
        this.members = new HashSet<>();
        this.workProjects = new ArrayList<>();
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateCategory(CategoryJpaEntity category) {
        this.category = category;
    }

    public void updateStatus(FolderStatus status) {
        this.status = status;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void updateMembers(Set<FolderMemberJpaEntity> newMembers) {
        this.members.clear();
        this.members.addAll(newMembers);
    }

    public void updateTeam(TeamJpaEntity team) {
        this.team = team;
    }
}
