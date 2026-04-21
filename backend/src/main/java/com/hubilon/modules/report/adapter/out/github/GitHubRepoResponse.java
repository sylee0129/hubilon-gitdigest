package com.hubilon.modules.report.adapter.out.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepoResponse(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName
) {}
