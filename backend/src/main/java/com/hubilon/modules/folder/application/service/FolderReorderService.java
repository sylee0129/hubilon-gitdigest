package com.hubilon.modules.folder.application.service;

import com.hubilon.modules.folder.application.dto.FolderReorderCommand;
import com.hubilon.modules.folder.domain.port.in.FolderReorderUseCase;
import com.hubilon.modules.folder.domain.port.out.FolderCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderReorderService implements FolderReorderUseCase {

    private final FolderCommandPort folderCommandPort;

    @Override
    public void reorder(FolderReorderCommand command) {
        folderCommandPort.updateSortOrders(command.orders());
    }
}
