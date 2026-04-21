package com.hubilon.modules.project.adapter.out.persistence;

import com.hubilon.modules.project.domain.model.GitProvider;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("프로젝트 이름")
    @Column(nullable = false)
    private String name;

    @Comment("GitLab 프로젝트 URL")
    @Column(nullable = false)
    private String gitlabUrl;

    @Comment("액세스 토큰")
    @Column(nullable = false)
    private String accessToken;

    @Comment("인증 방식 (PAT/OAUTH)")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthType authType;

    @Comment("GitLab 내부 프로젝트 ID")
    private Long gitlabProjectId;

    @Comment("정렬 순서")
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int sortOrder;

    @Comment("소속 폴더 (null이면 미분류)")
    @Column(name = "folder_id")
    private Long folderId;

    @Comment("소속 팀")
    @Column(name = "team_id")
    private Long teamId;

    @Comment("Git 제공자 (GITLAB/GITHUB)")
    @Column(name = "git_provider", nullable = false, columnDefinition = "VARCHAR(10) DEFAULT 'GITLAB'")
    @Enumerated(EnumType.STRING)
    private GitProvider gitProvider;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum AuthType {
        PAT, OAUTH
    }

    @Builder
    public ProjectJpaEntity(Long id, String name, String gitlabUrl, String accessToken,
                             AuthType authType, Long gitlabProjectId, int sortOrder, Long folderId, Long teamId,
                             GitProvider gitProvider) {
        this.id = id;
        this.name = name;
        this.gitlabUrl = gitlabUrl;
        this.accessToken = accessToken;
        this.authType = authType;
        this.gitlabProjectId = gitlabProjectId;
        this.sortOrder = sortOrder;
        this.folderId = folderId;
        this.teamId = teamId;
        this.gitProvider = gitProvider != null ? gitProvider : GitProvider.GITLAB;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
