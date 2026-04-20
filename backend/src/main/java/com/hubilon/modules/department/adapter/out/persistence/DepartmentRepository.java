package com.hubilon.modules.department.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<DepartmentJpaEntity, Long> {
}
