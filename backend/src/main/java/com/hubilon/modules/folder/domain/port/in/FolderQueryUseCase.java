package com.hubilon.modules.folder.domain.port.in;

import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;

import java.util.List;

public interface FolderQueryUseCase {
    List<FolderResult> searchAll(FolderStatus status, Long teamId);
}
