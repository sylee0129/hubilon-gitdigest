package com.hubilon.modules.folder.domain.port.in;

import com.hubilon.modules.folder.application.dto.FolderCreateCommand;
import com.hubilon.modules.folder.application.dto.FolderResult;

public interface FolderCreateUseCase {
    FolderResult create(FolderCreateCommand command);
}
