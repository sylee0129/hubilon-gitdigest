package com.hubilon.modules.auth.adapter.in.web;

import com.hubilon.auth.UserContext;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.common.response.Response;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController("meController")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserQueryUseCase userQueryUseCase;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보를 반환합니다.")
    @GetMapping("/me")
    public Response<LoginResponse.UserInfo> me() {
        String email = UserContext.getEmail();
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
