package com.hubilon.modules.category.application.service;

import com.hubilon.modules.category.application.dto.CategoryResult;
import com.hubilon.modules.category.domain.port.in.CategoryQueryUseCase;
import com.hubilon.modules.category.domain.port.out.CategoryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService implements CategoryQueryUseCase {

    private final CategoryQueryPort categoryQueryPort;

    @Override
    public List<CategoryResult> findAll() {
        return categoryQueryPort.findAllOrderBySortOrder().stream()
                .map(c -> new CategoryResult(c.getId(), c.getName(), c.getSortOrder()))
                .toList();
    }
}
