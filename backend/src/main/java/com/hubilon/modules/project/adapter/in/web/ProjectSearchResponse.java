package com.hubilon.modules.project.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hubilon.modules.project.domain.model.Project.AuthType;

import java.time.LocalDateTime;

public record ProjectSearchResponse(
        Long id,
        String name,
        String gitlabUrl,
        AuthType authType,
        Long gitlabProjectId,
        int sortOrder,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
