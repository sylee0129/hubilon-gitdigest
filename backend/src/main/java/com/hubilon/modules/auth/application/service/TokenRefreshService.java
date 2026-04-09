package com.hubilon.modules.auth.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.auth.domain.port.in.TokenRefreshUseCase;
import com.hubilon.modules.auth.domain.port.out.RefreshTokenPort;
import com.hubilon.modules.auth.domain.port.out.TokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenRefreshService implements TokenRefreshUseCase {

    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    public String refresh(String refreshToken) {
        if (!tokenPort.validateToken(refreshToken)) {
            throw new InvalidRequestException("유효하지 않은 리프레시 토큰입니다.");
        }

        if (!refreshTokenPort.existsByToken(refreshToken)) {
            throw new InvalidRequestException("만료되었거나 존재하지 않는 리프레시 토큰입니다.");
        }

        String email = refreshTokenPort.findEmailByToken(refreshToken)
                .orElseThrow(() -> new InvalidRequestException("리프레시 토큰에 해당하는 사용자를 찾을 수 없습니다."));

        return tokenPort.generateAccessToken(email);
    }
}
