package com.hubilon.modules.scheduler.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class SchedulerFolderResult {

    private Long id;
    private Long jobLogId;
    private Long folderId;
    private String folderName;
    private boolean success;
    private String errorMessage;
    private String confluencePageUrl;
}
