package com.hubilon.modules.folder.domain.port.in;

import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.application.dto.FolderUpdateCommand;

public interface FolderUpdateUseCase {
    FolderResult update(Long id, FolderUpdateCommand command);
}
