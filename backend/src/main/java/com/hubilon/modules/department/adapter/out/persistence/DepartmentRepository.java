package com.hubilon.modules.department.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentJpaEntity, Long> {
    Optional<DepartmentJpaEntity> findByName(String name);
}
