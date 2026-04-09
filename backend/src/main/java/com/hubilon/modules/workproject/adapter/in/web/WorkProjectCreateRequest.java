package com.hubilon.modules.workproject.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkProjectCreateRequest(@NotNull Long folderId, @NotBlank String name) {}
