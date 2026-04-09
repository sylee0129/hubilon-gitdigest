package com.hubilon.modules.folder.application.service;

import com.hubilon.common.exception.custom.ConflictException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.domain.port.in.FolderDeleteUseCase;
import com.hubilon.modules.folder.domain.port.out.FolderCommandPort;
import com.hubilon.modules.folder.domain.port.out.FolderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FolderDeleteService implements FolderDeleteUseCase {

    private final FolderCommandPort folderCommandPort;
    private final FolderQueryPort folderQueryPort;

    @Override
    public void delete(Long id, boolean force) {
        if (!folderQueryPort.existsById(id)) {
            throw new NotFoundException("폴더를 찾을 수 없습니다. id=" + id);
        }
        int workProjectCount = folderQueryPort.countWorkProjectsByFolderId(id);
        if (workProjectCount > 0 && !force) {
            throw new ConflictException(
                    "하위 세부 프로젝트가 " + workProjectCount + "개 존재합니다.",
                    new FolderDeleteConflictData(workProjectCount)
            );
        }
        folderCommandPort.deleteById(id);
    }

    public record FolderDeleteConflictData(int workProjectCount) {}
}
