package com.hubilon.modules.auth.adapter.in.web;

import com.hubilon.modules.user.domain.model.User;

public class LoginResponse {
    public record UserInfo(
            Long id,
            String name,
            String email,
            Long teamId,
            String teamName,
            User.Role role
    ) {}
}
