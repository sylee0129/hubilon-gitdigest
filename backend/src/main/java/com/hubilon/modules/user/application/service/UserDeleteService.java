package com.hubilon.modules.user.application.service;

import com.hubilon.common.exception.custom.ConflictException;
import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.user.domain.port.in.UserDeleteUseCase;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserFolderMemberPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserDeleteService implements UserDeleteUseCase {

    private final UserCommandPort userCommandPort;
    private final UserQueryPort userQueryPort;
    private final UserFolderMemberPort userFolderMemberPort;

    @Override
    public void delete(Long id) {
        if (!userQueryPort.existsById(id)) {
            throw new NotFoundException("사용자를 찾을 수 없습니다. id=" + id);
        }
        List<UserFolderMemberPort.FolderRef> refs = userFolderMemberPort.findFoldersByMemberId(id);
        if (!refs.isEmpty()) {
            throw new ConflictException(
                    "해당 사용자가 담당자로 지정된 프로젝트가 " + refs.size() + "개 존재합니다.",
                    new UserDeleteConflictData(refs)
            );
        }
        userCommandPort.deleteById(id);
    }

    public record UserDeleteConflictData(List<UserFolderMemberPort.FolderRef> referencedFolders) {}
}
