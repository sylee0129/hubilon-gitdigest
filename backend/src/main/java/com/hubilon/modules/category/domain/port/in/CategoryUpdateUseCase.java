package com.hubilon.modules.category.domain.port.in;

import com.hubilon.modules.category.application.dto.CategoryResult;
import com.hubilon.modules.category.application.dto.CategoryUpdateCommand;

public interface CategoryUpdateUseCase {
    CategoryResult update(Long id, CategoryUpdateCommand command);
}
