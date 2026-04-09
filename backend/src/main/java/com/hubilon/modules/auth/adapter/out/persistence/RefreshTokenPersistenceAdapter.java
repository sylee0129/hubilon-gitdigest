package com.hubilon.modules.auth.adapter.out.persistence;

import com.hubilon.modules.auth.domain.port.out.RefreshTokenPort;
import com.hubilon.modules.user.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public void save(Long userId, String token, LocalDateTime expiresAt) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(entity);
    }

    @Override
    public Optional<String> findEmailByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .flatMap(rt -> userRepository.findById(rt.getUserId()))
                .map(user -> user.getEmail());
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Override
    public boolean existsByToken(String token) {
        return refreshTokenRepository.existsByToken(token);
    }
}
