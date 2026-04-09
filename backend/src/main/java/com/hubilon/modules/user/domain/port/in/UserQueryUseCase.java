package com.hubilon.modules.user.domain.port.in;

import com.hubilon.modules.user.application.dto.UserSearchResult;
import com.hubilon.modules.user.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserQueryUseCase {

    List<UserSearchResult> searchAll();

    List<UserSearchResult> searchByQuery(String q);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
