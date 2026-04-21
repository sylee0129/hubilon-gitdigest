package com.hubilon.modules.project.adapter.in.web;

import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.project.domain.model.Project.AuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectRegisterRequest(
        @NotBlank(message = "GitLab URL은 필수입니다.")
        String gitlabUrl,

        Long gitlabProjectId, // 직접 입력 시 API 조회 생략 — read_repository 스코프 토큰으로도 동작

        String accessToken,   // 퍼블릭 프로젝트는 생략 가능

        @NotNull(message = "인증 방식은 필수입니다.")
        AuthType authType,

        GitProvider gitProvider  // null이면 GITLAB으로 처리
) {}
