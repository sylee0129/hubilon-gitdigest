package com.hubilon.modules.project.domain.port.in;

public interface ProjectMoveFolderUseCase {
    void moveToFolder(Long projectId, Long folderId);
}
