package com.hubilon.modules.user.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.user.application.dto.UserRegisterCommand;
import com.hubilon.modules.user.application.dto.UserRegisterResult;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserRegisterUseCase;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRegisterService implements UserRegisterUseCase {

    private final UserCommandPort userCommandPort;
    private final UserQueryPort userQueryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserRegisterResult register(UserRegisterCommand command) {
        if (userQueryPort.existsByEmail(command.email())) {
            throw new InvalidRequestException("이미 사용 중인 이메일입니다: " + command.email());
        }

        User user = User.builder()
                .name(command.name())
                .email(command.email())
                .password(passwordEncoder.encode(command.password()))
                .teamId(command.teamId())
                .role(command.role() != null ? command.role() : User.Role.USER)
                .build();

        User saved = userCommandPort.save(user);

        return new UserRegisterResult(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getTeamId(),
                saved.getTeamName(),
                saved.getRole()
        );
    }
}
