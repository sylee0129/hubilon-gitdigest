package com.hubilon.modules.report.adapter.out.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GitHubCommitDetail(
        String sha,
        List<GitHubCommitDetail.FileEntry> files
) {
    public record FileEntry(
            String filename,
            @JsonProperty("previous_filename") String previousFilename,
            String status,
            int additions,
            int deletions
    ) {}
}
