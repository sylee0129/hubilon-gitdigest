package com.hubilon.modules.confluence.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfluenceSpaceConfigRepository extends JpaRepository<ConfluenceSpaceConfigJpaEntity, Long> {

    Optional<ConfluenceSpaceConfigJpaEntity> findByDeptId(Long deptId);
}
