package com.hubilon.modules.report.adapter.out.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabDiffResponse(
        @JsonProperty("old_path") String oldPath,
        @JsonProperty("new_path") String newPath,
        @JsonProperty("new_file") boolean newFile,
        @JsonProperty("renamed_file") boolean renamedFile,
        @JsonProperty("deleted_file") boolean deletedFile,
        String diff
) {}
