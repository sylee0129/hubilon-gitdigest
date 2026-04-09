package com.hubilon.modules.auth.domain.port.in;

public interface TokenRefreshUseCase {

    String refresh(String refreshToken);
}
