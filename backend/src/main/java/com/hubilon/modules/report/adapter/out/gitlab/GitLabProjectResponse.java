package com.hubilon.modules.report.adapter.out.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabProjectResponse(
        Long id,
        String name,
        @JsonProperty("path_with_namespace") String pathWithNamespace
) {}
