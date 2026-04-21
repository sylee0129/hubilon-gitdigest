package com.hubilon.modules.confluence.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamConfigResponse(
        Long id,
        Long teamId,
        String teamName,
        String parentPageId,
        String updatedBy,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {}
