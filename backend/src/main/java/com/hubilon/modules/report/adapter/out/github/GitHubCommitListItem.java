package com.hubilon.modules.report.adapter.out.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubCommitListItem(
        String sha,
        @JsonProperty("commit") GitHubCommitListItem.CommitData commit
) {
    public record CommitData(
            @JsonProperty("author") AuthorData author,
            String message
    ) {}

    public record AuthorData(
            String name,
            String email,
            String date
    ) {}
}
