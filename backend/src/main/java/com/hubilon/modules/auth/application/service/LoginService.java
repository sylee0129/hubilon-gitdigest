package com.hubilon.modules.auth.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.auth.application.dto.LoginCommand;
import com.hubilon.modules.auth.domain.model.TokenPair;
import com.hubilon.modules.auth.domain.port.in.LoginUseCase;
import com.hubilon.modules.auth.domain.port.out.RefreshTokenPort;
import com.hubilon.modules.auth.domain.port.out.TokenPort;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService implements LoginUseCase {

    private final UserQueryPort userQueryPort;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpiry;

    @Override
    public TokenPair login(LoginCommand command) {
        User user = userQueryPort.findByEmail(command.email())
                .orElseThrow(() -> new InvalidRequestException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new InvalidRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = tokenPort.generateAccessToken(user.getEmail());
        String refreshToken = tokenPort.generateRefreshToken(user.getEmail());

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiry);
        refreshTokenPort.save(user.getId(), refreshToken, expiresAt);

        return new TokenPair(accessToken, refreshToken, refreshTokenExpiry);
    }
}
