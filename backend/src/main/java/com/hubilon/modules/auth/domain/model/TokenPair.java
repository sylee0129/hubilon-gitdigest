package com.hubilon.modules.auth.domain.model;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
