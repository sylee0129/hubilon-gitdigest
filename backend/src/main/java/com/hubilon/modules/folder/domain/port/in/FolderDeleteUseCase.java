package com.hubilon.modules.folder.domain.port.in;

public interface FolderDeleteUseCase {
    void delete(Long id, boolean force);
}
