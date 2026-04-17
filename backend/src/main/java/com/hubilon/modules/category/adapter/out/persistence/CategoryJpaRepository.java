package com.hubilon.modules.category.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {
    List<CategoryJpaEntity> findAllByOrderBySortOrderAsc();

    @Query("SELECT COALESCE(MAX(c.sortOrder), -1) FROM CategoryJpaEntity c")
    int findMaxSortOrder();
}
