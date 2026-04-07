package com.hubilon.modules.project.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Project {

    private Long id;
    private String name;
    private String gitlabUrl;
    private String accessToken;
    private AuthType authType;
    private Long gitlabProjectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AuthType {
        PAT, OAUTH
    }
}
