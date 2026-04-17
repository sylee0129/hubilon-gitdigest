package com.hubilon.modules.category.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(@NotBlank String name) {}
