package com.hubilon.modules.workproject.application.dto;

import java.util.List;

public record WorkProjectReorderCommand(Long folderId, List<OrderItem> orders) {
    public record OrderItem(Long id, int sortOrder) {}
}
