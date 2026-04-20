package com.hubilon.modules.folder.adapter.out.persistence;

import com.hubilon.modules.folder.domain.model.FolderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderJpaRepository extends JpaRepository<FolderJpaEntity, Long> {

    @Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects ORDER BY f.sortOrder ASC")
    List<FolderJpaEntity> findAllWithDetails();

    @Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.status = :status ORDER BY f.sortOrder ASC")
    List<FolderJpaEntity> findAllWithDetailsByStatus(@Param("status") FolderStatus status);

    @Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.id = :id")
    Optional<FolderJpaEntity> findWithDetailsById(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.team.id = :teamId ORDER BY f.sortOrder ASC")
    List<FolderJpaEntity> findAllWithDetailsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.team.id = :teamId AND f.status = :status ORDER BY f.sortOrder ASC")
    List<FolderJpaEntity> findAllWithDetailsByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") FolderStatus status);
}
