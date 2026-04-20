package com.hubilon.modules.auth.application.dto;

public record LoginResult(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        String name,
        String email,
        Long teamId,
        String teamName,
        String role
) {}
