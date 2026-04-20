package com.hubilon.modules.project.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {

    List<ProjectJpaEntity> findAllByOrderBySortOrderAsc();

    List<ProjectJpaEntity> findByFolderIdOrderBySortOrderAsc(Long folderId);

    List<ProjectJpaEntity> findAllByTeamIdOrderBySortOrderAsc(Long teamId);
}
