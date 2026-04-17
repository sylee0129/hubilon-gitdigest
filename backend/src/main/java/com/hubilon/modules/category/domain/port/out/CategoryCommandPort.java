package com.hubilon.modules.category.domain.port.out;

import com.hubilon.modules.category.domain.model.Category;

public interface CategoryCommandPort {
    Category save(Category category);
}
