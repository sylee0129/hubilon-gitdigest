package com.hubilon.modules.auth.domain.port.in;

public interface LogoutUseCase {

    void logout(String refreshToken);
}
