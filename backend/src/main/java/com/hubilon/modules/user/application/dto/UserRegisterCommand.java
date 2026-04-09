package com.hubilon.modules.user.application.dto;

import com.hubilon.modules.user.domain.model.User;

public record UserRegisterCommand(
        String name,
        String email,
        String password,
        String department,
        User.Role role
) {}
