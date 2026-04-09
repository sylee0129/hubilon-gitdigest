package com.hubilon.modules.auth.application.service;

import com.hubilon.modules.auth.domain.port.in.LogoutUseCase;
import com.hubilon.modules.auth.domain.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenPort refreshTokenPort;

    @Override
    public void logout(String refreshToken) {
        refreshTokenPort.deleteByToken(refreshToken);
    }
}
