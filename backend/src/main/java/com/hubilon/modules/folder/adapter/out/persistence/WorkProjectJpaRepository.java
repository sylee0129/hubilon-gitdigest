package com.hubilon.modules.folder.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkProjectJpaRepository extends JpaRepository<WorkProjectJpaEntity, Long> {
    List<WorkProjectJpaEntity> findByFolderIdOrderBySortOrderAsc(Long folderId);
}
