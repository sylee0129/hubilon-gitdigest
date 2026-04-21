package com.hubilon.modules.confluence.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SpaceConfigUpsertRequest(
        @NotNull(message = "deptId는 필수입니다.")
        Long deptId,
        @NotBlank(message = "userEmail은 필수입니다.")
        String userEmail,
        @NotBlank(message = "apiToken은 필수입니다.")
        String apiToken,
        @NotBlank(message = "spaceKey는 필수입니다.")
        String spaceKey,
        @NotBlank(message = "baseUrl은 필수입니다.")
        String baseUrl
) {}
