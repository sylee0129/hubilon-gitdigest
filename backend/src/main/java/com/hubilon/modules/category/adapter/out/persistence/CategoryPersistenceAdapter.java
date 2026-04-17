package com.hubilon.modules.category.adapter.out.persistence;

import com.hubilon.modules.category.domain.model.Category;
import com.hubilon.modules.category.domain.port.out.CategoryCommandPort;
import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryCommandPort, CategoryQueryPort {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity;
        if (category.getId() != null) {
            entity = categoryJpaRepository.findById(category.getId())
                    .orElse(CategoryJpaEntity.builder()
                            .name(category.getName())
                            .sortOrder(category.getSortOrder())
                            .build());
            entity.updateName(category.getName());
            entity.updateSortOrder(category.getSortOrder());
        } else {
            entity = CategoryJpaEntity.builder()
                    .name(category.getName())
                    .sortOrder(category.getSortOrder())
                    .build();
        }
        CategoryJpaEntity saved = categoryJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<Category> findAllOrderBySortOrder() {
        return categoryJpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public int findMaxSortOrder() {
        return categoryJpaRepository.findMaxSortOrder();
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
