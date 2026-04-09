package com.hubilon.modules.user.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.user.application.dto.UserSearchResult;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryUseCase userQueryUseCase;

    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 반환합니다.")
    @GetMapping
    public Response<List<UserSearchResult>> searchAll() {
        return Response.ok(userQueryUseCase.searchAll());
    }
}
