package com.hubilon.modules.category.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.category.application.dto.CategoryCreateCommand;
import com.hubilon.modules.category.application.dto.CategoryResult;
import com.hubilon.modules.category.application.dto.CategoryUpdateCommand;
import com.hubilon.modules.category.domain.model.Category;
import com.hubilon.modules.category.domain.port.in.CategoryCreateUseCase;
import com.hubilon.modules.category.domain.port.in.CategoryUpdateUseCase;
import com.hubilon.modules.category.domain.port.out.CategoryCommandPort;
import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService implements CategoryCreateUseCase, CategoryUpdateUseCase {

    private final CategoryCommandPort categoryCommandPort;
    private final CategoryQueryPort categoryQueryPort;

    @Override
    public CategoryResult create(CategoryCreateCommand command) {
        int nextOrder = categoryQueryPort.findMaxSortOrder() + 1;
        Category category = Category.builder()
                .name(command.name())
                .sortOrder(nextOrder)
                .build();
        Category saved = categoryCommandPort.save(category);
        return new CategoryResult(saved.getId(), saved.getName(), saved.getSortOrder());
    }

    @Override
    public CategoryResult update(Long id, CategoryUpdateCommand command) {
        Category existing = categoryQueryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리를 찾을 수 없습니다. id=" + id));
        Category updated = Category.builder()
                .id(existing.getId())
                .name(command.name())
                .sortOrder(existing.getSortOrder())
                .build();
        Category saved = categoryCommandPort.save(updated);
        return new CategoryResult(saved.getId(), saved.getName(), saved.getSortOrder());
    }
}
