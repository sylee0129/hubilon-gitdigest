package com.hubilon.modules.department.adapter.in.web;

import com.hubilon.modules.team.adapter.in.web.TeamResponse;

import java.util.List;

public record DepartmentResponse(Long id, String name, List<TeamResponse> teams) {}
