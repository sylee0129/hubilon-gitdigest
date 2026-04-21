package com.hubilon.modules.confluence.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfluenceTeamConfigRepository extends JpaRepository<ConfluenceTeamConfigJpaEntity, Long> {

    Optional<ConfluenceTeamConfigJpaEntity> findByTeamId(Long teamId);
}
