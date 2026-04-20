package com.hubilon.modules.project.application.dto;

import com.hubilon.modules.project.domain.model.Project.AuthType;

public record ProjectRegisterCommand(
        String gitlabUrl,
        Long gitlabProjectId,
        String accessToken,
        AuthType authType,
        Long teamId
) {}
