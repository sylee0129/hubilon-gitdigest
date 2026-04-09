package com.hubilon.modules.folder.domain.port.out;

import com.hubilon.modules.folder.application.dto.FolderReorderCommand;
import com.hubilon.modules.folder.domain.model.Folder;

import java.util.List;

public interface FolderCommandPort {
    Folder save(Folder folder, List<Long> memberIds);
    void deleteById(Long id);
    void updateSortOrders(List<FolderReorderCommand.FolderOrderItem> orders);
}
