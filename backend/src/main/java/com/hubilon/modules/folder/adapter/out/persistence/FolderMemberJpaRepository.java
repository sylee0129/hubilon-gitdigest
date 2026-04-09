package com.hubilon.modules.folder.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderMemberJpaRepository extends JpaRepository<FolderMemberJpaEntity, FolderMemberJpaEntity.FolderMemberId> {
    List<FolderMemberJpaEntity> findByUserId(Long userId);
    void deleteByFolderIdAndUserIdIn(Long folderId, List<Long> userIds);
    void deleteByFolderId(Long folderId);
}
