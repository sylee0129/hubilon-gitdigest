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
@Table(name = "confluence_space_configs")
@NoArgsConstructor(access = PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ConfluenceSpaceConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("departments.id 참조")
    @Column(nullable = false, unique = true)
    private Long deptId;

    @Comment("Confluence 계정 이메일")
    @Column(nullable = false)
    private String userEmail;

    @Comment("AES-256-GCM 암호화된 API 토큰 (Base64(IV):Base64(CipherText+AuthTag))")
    @Column(nullable = false, length = 1000)
    private String apiToken;

    @Comment("Confluence 스페이스 키")
    @Column(nullable = false, length = 100)
    private String spaceKey;

    @Comment("Confluence Base URL")
    @Column(nullable = false, length = 500)
    private String baseUrl;

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
    public ConfluenceSpaceConfigJpaEntity(Long id, Long deptId, String userEmail,
                                          String apiToken, String spaceKey, String baseUrl,
                                          String createdBy, String updatedBy) {
        this.id = id;
        this.deptId = deptId;
        this.userEmail = userEmail;
        this.apiToken = apiToken;
        this.spaceKey = spaceKey;
        this.baseUrl = baseUrl;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public void update(String userEmail, String apiToken, String spaceKey,
                       String baseUrl, String updatedBy) {
        this.userEmail = userEmail;
        this.apiToken = apiToken;
        this.spaceKey = spaceKey;
        this.baseUrl = baseUrl;
        this.updatedBy = updatedBy;
    }
}
