package com.hubilon.modules.report.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileChange {

    private String oldPath;
    private String newPath;
    private boolean newFile;
    private boolean renamedFile;
    private boolean deletedFile;
    private int addedLines;
    private int removedLines;
}
