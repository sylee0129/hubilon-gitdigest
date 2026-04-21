package com.hubilon.modules.confluence.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TeamConfigUpsertRequest(
        @NotNull(message = "teamId는 필수입니다.")
        Long teamId,
        @NotBlank(message = "parentPageId는 필수입니다.")
        String parentPageId
) {}
