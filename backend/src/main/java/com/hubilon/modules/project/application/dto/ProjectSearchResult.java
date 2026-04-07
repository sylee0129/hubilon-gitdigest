package com.hubilon.modules.project.application.dto;

import com.hubilon.modules.project.domain.model.Project.AuthType;

import java.time.LocalDateTime;

public record ProjectSearchResult(
        Long id,
        String name,
        String gitlabUrl,
        AuthType authType,
        Long gitlabProjectId,
        LocalDateTime createdAt
) {}
