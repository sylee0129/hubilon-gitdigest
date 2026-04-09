package com.hubilon.modules.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE (:q IS NULL OR u.name LIKE %:q% OR u.email LIKE %:q%)")
    List<UserJpaEntity> findByQuery(@Param("q") String q);
}
