package com.hubilon.modules.report.application.dto;

public record FileChangeResult(
        String oldPath,
        String newPath,
        boolean newFile,
        boolean renamedFile,
        boolean deletedFile,
        int addedLines,
        int removedLines
) {}
