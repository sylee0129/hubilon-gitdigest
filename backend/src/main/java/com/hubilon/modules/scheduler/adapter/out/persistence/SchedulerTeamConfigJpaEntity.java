package com.hubilon.modules.scheduler.adapter.out.persistence;

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
@Table(name = "scheduler_team_configs")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SchedulerTeamConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("팀 ID")
    @Column(name = "team_id", nullable = false, unique = true)
    private Long teamId;

    @Comment("팀 이름")
    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    @Comment("스케줄러 활성 여부")
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public SchedulerTeamConfigJpaEntity(Long id, Long teamId, String teamName, boolean enabled) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.enabled = enabled;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
