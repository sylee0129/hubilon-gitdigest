package com.hubilon.modules.project.application.dto;

import java.util.List;

public record ProjectReorderCommand(List<Long> projectIds) {}
