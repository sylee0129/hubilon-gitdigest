package com.hubilon.modules.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "리프레시 토큰을 입력해주세요.")
        String refreshToken
) {}
