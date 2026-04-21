package com.hubilon.modules.folder.adapter.in.web;

import com.hubilon.modules.folder.application.dto.FolderCreateCommand;
import com.hubilon.modules.folder.application.dto.FolderReorderCommand;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.application.dto.FolderUpdateCommand;
import org.springframework.stereotype.Component;

@Component
public class FolderWebMapper {

    public FolderCreateCommand toCreateCommand(FolderCreateRequest req) {
        return new FolderCreateCommand(req.name(), req.categoryId(), req.status(), req.memberIds(), req.teamId());
    }

    public FolderUpdateCommand toUpdateCommand(FolderUpdateRequest req) {
        return new FolderUpdateCommand(req.name(), req.categoryId(), req.status(), req.memberIds(), req.teamId());
    }

    public FolderReorderCommand toReorderCommand(FolderReorderRequest req) {
        return new FolderReorderCommand(
                req.orders().stream()
                        .map(o -> new FolderReorderCommand.FolderOrderItem(o.id(), o.sortOrder()))
                        .toList()
        );
    }

    public FolderResponse toResponse(FolderResult result) {
        return new FolderResponse(
                result.id(), result.name(), result.categoryId(), result.categoryName(),
                result.status(), result.sortOrder(), result.teamId(),
                result.members(), result.workProjects()
        );
    }
}
