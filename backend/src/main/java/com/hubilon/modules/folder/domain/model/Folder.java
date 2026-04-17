package com.hubilon.modules.folder.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Folder {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private FolderStatus status;
    private int sortOrder;
}
