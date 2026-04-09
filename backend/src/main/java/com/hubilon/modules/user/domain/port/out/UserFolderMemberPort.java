package com.hubilon.modules.user.domain.port.out;

import java.util.List;

public interface UserFolderMemberPort {
    List<FolderRef> findFoldersByMemberId(Long userId);

    record FolderRef(Long id, String name) {}
}
