package com.hubilon.modules.folder.application.dto;

import java.util.List;

public record FolderReorderCommand(List<FolderOrderItem> orders) {
    public record FolderOrderItem(Long id, int sortOrder) {}
}
