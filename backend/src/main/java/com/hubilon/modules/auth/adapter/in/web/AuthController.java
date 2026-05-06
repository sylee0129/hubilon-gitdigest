package com.hubilon.modules.auth.adapter.in.web;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.common.response.Response;
import com.hubilon.common.security.JwtClaimExtractor;
import com.hubilon.modules.auth.application.dto.LoginCommand;
import com.hubilon.modules.auth.domain.model.TokenPair;
import com.hubilon.modules.auth.domain.port.in.LoginUseCase;
import com.hubilon.modules.auth.domain.port.in.LogoutUseCase;
import com.hubilon.modules.auth.domain.port.in.TokenRefreshUseCase;
import com.hubilon.modules.user.adapter.out.persistence.UserJpaEntity;
import com.hubilon.modules.user.adapter.out.persistence.UserRepository;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final UserRepository userRepository;

    @Value("${keycloak.client-id}")
    private String clientId;

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

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 반환합니다. (Keycloak JWT claims 기반)")
    @GetMapping("/me")
    public Response<UserInfoResponse> me(Authentication authentication) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        String email = jwt.getClaimAsString("email");

        // 첫 로그인 자동 프로비저닝
        if (!userRepository.existsByEmail(email)) {
            UserJpaEntity newUser = UserJpaEntity.builder()
                    .name(jwt.getClaimAsString("preferred_username"))
                    .email(email)
                    .password("")
                    .role(UserJpaEntity.Role.USER)
                    .build();
            userRepository.save(newUser);
        }

        List<String> roles = JwtClaimExtractor.extractClientRoles(jwt, clientId);
        String role = roles.contains("ROLE_ADMIN") ? "ROLE_ADMIN" : "ROLE_USER";

        UserInfoResponse response = new UserInfoResponse(
                jwt.getClaimAsString("preferred_username"),
                email,
                role,
                JwtClaimExtractor.extractDepartmentName(jwt),
                JwtClaimExtractor.extractTeamName(jwt)
        );
        return Response.ok(response);
    }
}
