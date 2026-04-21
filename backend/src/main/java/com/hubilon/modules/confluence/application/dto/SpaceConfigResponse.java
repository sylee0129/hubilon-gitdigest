package com.hubilon.modules.confluence.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SpaceConfigResponse(
        Long id,
        Long deptId,
        String deptName,
        String userEmail,
        String apiToken,
        String spaceKey,
        String baseUrl,
        String updatedBy,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {}
