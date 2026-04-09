package com.hubilon.modules.auth.domain.port.out;

public interface TokenPort {

    String generateAccessToken(String email);

    String generateRefreshToken(String email);

    boolean validateToken(String token);

    String extractEmail(String token);
}
