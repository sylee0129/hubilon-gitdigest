package com.hubilon.modules.report.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommitInfo {

    private Long id;
    private String sha;
    private String authorName;
    private String authorEmail;
    private LocalDateTime committedAt;
    private String message;
    private List<FileChange> fileChanges;
}
