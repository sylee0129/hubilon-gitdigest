package com.hubilon.modules.auth.adapter.in.web;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.common.response.Response;
import com.hubilon.modules.auth.application.dto.LoginCommand;
import com.hubilon.modules.auth.domain.model.TokenPair;
import com.hubilon.modules.auth.domain.port.in.LoginUseCase;
import com.hubilon.modules.auth.domain.port.in.LogoutUseCase;
import com.hubilon.modules.auth.domain.port.in.TokenRefreshUseCase;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final TokenRefreshUseCase tokenRefreshUseCase;
    private final UserQueryUseCase userQueryUseCase;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
    @PostMapping("/login")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = loginUseCase.login(new LoginCommand(request.email(), request.password()));

        User user = userQueryUseCase.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        LoginResponse response = new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn(),
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getTeamId(),
                        user.getTeamName(),
                        user.getRole()
                )
        );

        return Response.ok(response);
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    @PostMapping("/refresh")
    public Response<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest request) {
        String newAccessToken = tokenRefreshUseCase.refresh(request.refreshToken());
        return Response.ok(Map.of("accessToken", newAccessToken));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public Response<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutUseCase.logout(request.refreshToken());
        return Response.ok();
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 반환합니다.")
    @GetMapping("/me")
    public Response<LoginResponse.UserInfo> me() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userQueryUseCase.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        return Response.ok(new LoginResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTeamId(),
                user.getTeamName(),
                user.getRole()
        ));
    }
}
