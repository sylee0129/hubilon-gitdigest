package com.hubilon.common.security;

import com.hubilon.auth.UserContext;
import com.hubilon.modules.user.application.service.UserProvisioningService;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserQueryPort userQueryPort;
    private final UserProvisioningService userProvisioningService;

    public User getCurrentUser() {
        String email = UserContext.getEmail();
        return userQueryPort.findByEmail(email)
                .orElseGet(() -> userProvisioningService.provisionOrSync(email));
    }
}
