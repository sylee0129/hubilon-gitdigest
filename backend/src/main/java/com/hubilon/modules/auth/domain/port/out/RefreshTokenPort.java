package com.hubilon.modules.auth.domain.port.out;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenPort {

    void save(Long userId, String token, LocalDateTime expiresAt);

    Optional<String> findEmailByToken(String token);

    void deleteByToken(String token);

    boolean existsByToken(String token);
}
