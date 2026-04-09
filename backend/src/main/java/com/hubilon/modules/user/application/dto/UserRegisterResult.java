package com.hubilon.modules.user.application.dto;

import com.hubilon.modules.user.domain.model.User;

public record UserRegisterResult(
        Long id,
        String name,
        String email,
        String department,
        User.Role role
) {}
