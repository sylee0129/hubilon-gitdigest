package com.hubilon.modules.folder.domain.port.in;

import com.hubilon.modules.folder.application.dto.FolderReorderCommand;

public interface FolderReorderUseCase {
    void reorder(FolderReorderCommand command);
}
