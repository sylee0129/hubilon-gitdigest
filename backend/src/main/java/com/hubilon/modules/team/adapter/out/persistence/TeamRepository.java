package com.hubilon.modules.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamJpaEntity, Long> {
    Optional<TeamJpaEntity> findByName(String name);
}
