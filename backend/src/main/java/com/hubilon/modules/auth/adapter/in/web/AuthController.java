package com.hubilon.modules.auth.adapter.in.web;

import com.hubilon.auth.UserContext;
import com.hubilon.common.response.Response;
import com.hubilon.modules.user.application.service.UserProvisioningService;
import com.hubilon.modules.user.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController("meController")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserProvisioningService userProvisioningService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 반환합니다.")
    @GetMapping("/me")
    public Response<LoginResponse.UserInfo> me() {
        String email = UserContext.getEmail();
        User user = userProvisioningService.provisionOrSync(email);

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