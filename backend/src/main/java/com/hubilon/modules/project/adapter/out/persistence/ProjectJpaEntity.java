package com.hubilon.modules.project.adapter.out.persistence;

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
                             AuthType authType, Long gitlabProjectId) {
        this.id = id;
        this.name = name;
        this.gitlabUrl = gitlabUrl;
        this.accessToken = accessToken;
        this.authType = authType;
        this.gitlabProjectId = gitlabProjectId;
    }
}
