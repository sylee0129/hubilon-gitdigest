package com.hubilon.modules.category.domain.port.in;

import com.hubilon.modules.category.application.dto.CategoryCreateCommand;
import com.hubilon.modules.category.application.dto.CategoryResult;

public interface CategoryCreateUseCase {
    CategoryResult create(CategoryCreateCommand command);
}
