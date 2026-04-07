package com.hubilon.modules.report.adapter.out.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabCommitResponse(
        String id,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_email") String authorEmail,
        @JsonProperty("committed_date") String committedDate,
        String title,
        String message
) {}
