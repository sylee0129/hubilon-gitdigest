package com.hubilon.modules.user.domain.port.out;

import com.hubilon.modules.user.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserQueryPort {

    List<User> findAll();

    List<User> findByQuery(String q);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsById(Long id);
}
