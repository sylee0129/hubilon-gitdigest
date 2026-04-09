package com.hubilon.modules.workproject.adapter.in.web;

import java.util.List;

public record WorkProjectReorderRequest(Long folderId, List<OrderItem> orders) {
    public record OrderItem(Long id, int sortOrder) {}
}
