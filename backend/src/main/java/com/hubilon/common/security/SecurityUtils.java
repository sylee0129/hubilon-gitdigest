package com.hubilon.common.security;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserQueryPort userQueryPort;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userQueryPort.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
