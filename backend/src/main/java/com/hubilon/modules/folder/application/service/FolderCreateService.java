package com.hubilon.modules.folder.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.application.dto.FolderCreateCommand;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.Folder;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderCreateUseCase;
import com.hubilon.modules.folder.domain.port.out.FolderCommandPort;
import com.hubilon.modules.folder.domain.port.out.FolderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderCreateService implements FolderCreateUseCase {

    private final FolderCommandPort folderCommandPort;
    private final FolderQueryPort folderQueryPort;

    @Override
    public FolderResult create(FolderCreateCommand command) {
        Folder folder = Folder.builder()
                .name(command.name())
                .category(command.category())
                .status(command.status() != null ? command.status() : FolderStatus.IN_PROGRESS)
                .sortOrder(0)
                .build();
        Folder saved = folderCommandPort.save(folder, command.memberIds() != null ? command.memberIds() : List.of());
        return folderQueryPort.findWithDetailsById(saved.getId())
                .orElseThrow(() -> new NotFoundException("폴더를 찾을 수 없습니다."));
    }
}
