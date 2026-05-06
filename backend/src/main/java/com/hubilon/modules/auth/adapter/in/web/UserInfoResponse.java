package com.hubilon.modules.auth.adapter.in.web;

public record UserInfoResponse(
        String username,
        String email,
        String role,
        String departmentName,
        String teamName
) {}
