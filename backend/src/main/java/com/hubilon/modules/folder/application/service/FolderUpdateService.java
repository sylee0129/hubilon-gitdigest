package com.hubilon.modules.folder.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.application.dto.FolderUpdateCommand;
import com.hubilon.modules.folder.domain.model.Folder;
import com.hubilon.modules.folder.domain.port.in.FolderUpdateUseCase;
import com.hubilon.modules.folder.domain.port.out.FolderCommandPort;
import com.hubilon.modules.folder.domain.port.out.FolderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderUpdateService implements FolderUpdateUseCase {

    private final FolderCommandPort folderCommandPort;
    private final FolderQueryPort folderQueryPort;

    @Override
    public FolderResult update(Long id, FolderUpdateCommand command) {
        FolderResult existing = folderQueryPort.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("폴더를 찾을 수 없습니다. id=" + id));
        Folder folder = Folder.builder()
                .id(id)
                .name(command.name())
                .category(command.category())
                .status(command.status())
                .sortOrder(existing.sortOrder())
                .build();
        folderCommandPort.save(folder, command.memberIds() != null ? command.memberIds() : List.of());
        return folderQueryPort.findWithDetailsById(id)
                .orElseThrow();
    }
}
