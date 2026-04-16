package com.hubilon.modules.user.application.service;

import com.hubilon.modules.user.application.dto.UserSearchResult;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.in.UserQueryUseCase;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService implements UserQueryUseCase {

    private final UserQueryPort userQueryPort;

    @Override
    public List<UserSearchResult> searchAll() {
        return userQueryPort.findAll().stream()
                .map(u -> new UserSearchResult(u.getId(), u.getName(), u.getEmail(), u.getTeamId(), u.getTeamName(), u.getRole()))
                .toList();
    }

    @Override
    public List<UserSearchResult> searchByQuery(String q) {
        return userQueryPort.findByQuery(q).stream()
                .map(u -> new UserSearchResult(u.getId(), u.getName(), u.getEmail(), u.getTeamId(), u.getTeamName(), u.getRole()))
                .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userQueryPort.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userQueryPort.existsByEmail(email);
    }
}
