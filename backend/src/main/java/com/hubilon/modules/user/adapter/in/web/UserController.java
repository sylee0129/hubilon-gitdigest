package com.hubilon.modules.user.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.user.application.dto.UserRegisterCommand;
import com.hubilon.modules.user.application.dto.UserRegisterResult;
import com.hubilon.modules.user.application.dto.UserSearchResult;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserDeleteUseCase;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import com.hubilon.modules.user.domain.port.in.UserRegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryUseCase userQueryUseCase;
    private final UserRegisterUseCase userRegisterUseCase;
    private final UserDeleteUseCase userDeleteUseCase;

    @Operation(summary = "사용자 목록 조회", description = "전체 또는 검색어로 사용자 목록을 반환합니다.")
    @GetMapping
    public Response<List<UserSearchResult>> search(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return Response.ok(userQueryUseCase.searchAll());
        }
        return Response.ok(userQueryUseCase.searchByQuery(q));
    }

    @Operation(summary = "사용자 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<UserRegisterResult> register(@Valid @RequestBody UserRegisterRequest request) {
        UserRegisterCommand command = new UserRegisterCommand(
                request.name(), request.email(), request.password(), request.teamId(), User.Role.USER
        );
        return Response.ok(userRegisterUseCase.register(command));
    }

    @Operation(summary = "사용자 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response<Void> delete(@PathVariable Long id) {
        userDeleteUseCase.delete(id);
        return Response.ok();
    }
}
