package com.hubilon.modules.department.adapter.out.persistence;

import com.hubilon.modules.department.domain.model.Department;
import com.hubilon.modules.department.domain.port.out.DepartmentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DepartmentPersistenceAdapter implements DepartmentQueryPort {

    private final DepartmentRepository departmentRepository;

    @Override
    public List<Department> findAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id).map(this::toDomain);
    }

    private Department toDomain(DepartmentJpaEntity entity) {
        return Department.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
