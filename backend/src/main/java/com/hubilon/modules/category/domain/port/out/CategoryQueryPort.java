package com.hubilon.modules.category.domain.port.out;

import com.hubilon.modules.category.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryQueryPort {
    List<Category> findAllOrderBySortOrder();
    Optional<Category> findById(Long id);
    int findMaxSortOrder();
}
