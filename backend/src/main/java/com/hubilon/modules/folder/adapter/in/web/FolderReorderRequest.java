package com.hubilon.modules.folder.adapter.in.web;

import java.util.List;

public record FolderReorderRequest(List<OrderItem> orders) {
    public record OrderItem(Long id, int sortOrder) {}
}
