package com.hubilon.modules.category.domain.port.in;

import com.hubilon.modules.category.application.dto.CategoryResult;

import java.util.List;

public interface CategoryQueryUseCase {
    List<CategoryResult> findAll();
}
