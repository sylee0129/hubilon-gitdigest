package com.hubilon.modules.folder.application.service;

import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.in.FolderQueryUseCase;
import com.hubilon.modules.folder.domain.port.out.FolderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderQueryService implements FolderQueryUseCase {

    private final FolderQueryPort folderQueryPort;

    @Override
    public List<FolderResult> searchAll(FolderStatus status) {
        return folderQueryPort.findAllWithDetails(status);
    }
}
