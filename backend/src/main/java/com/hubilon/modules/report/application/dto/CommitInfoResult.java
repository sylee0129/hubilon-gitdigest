package com.hubilon.modules.report.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommitInfoResult(
        Long id,
        String sha,
        String authorName,
        String authorEmail,
        LocalDateTime committedAt,
        String message,
        List<FileChangeResult> fileChanges
) {}
