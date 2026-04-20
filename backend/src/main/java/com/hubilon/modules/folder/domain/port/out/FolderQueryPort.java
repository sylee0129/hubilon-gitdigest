package com.hubilon.modules.folder.domain.port.out;

import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;
import java.util.Optional;

public interface FolderQueryPort {
    List<FolderResult> findAllWithDetails(FolderStatus status, Long teamId);
    Optional<FolderResult> findWithDetailsById(Long id);
    boolean existsById(Long id);
    int countWorkProjectsByFolderId(Long folderId);
}
