package com.hubilon.modules.confluence.adapter.out.persistence;

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
@Table(name = "confluence_team_configs")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ConfluenceTeamConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("teams.id 참조")
    @Column(nullable = false, unique = true)
    private Long teamId;

    @Comment("Confluence 상위 페이지 ID")
    @Column(nullable = false, length = 100)
    private String parentPageId;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder(toBuilder = true)
    public ConfluenceTeamConfigJpaEntity(Long id, Long teamId, String parentPageId,
                                         String createdBy, String updatedBy) {
        this.id = id;
        this.teamId = teamId;
        this.parentPageId = parentPageId;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public void update(String parentPageId, String updatedBy) {
        this.parentPageId = parentPageId;
        this.updatedBy = updatedBy;
    }
}
