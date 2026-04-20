package com.hubilon.modules.department.application.dto;

import java.util.List;

public record DepartmentWithTeamsResult(Long id, String name, List<TeamResult> teams) {}
