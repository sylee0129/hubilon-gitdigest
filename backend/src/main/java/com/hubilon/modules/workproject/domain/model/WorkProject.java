package com.hubilon.modules.workproject.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class WorkProject {
    private Long id;
    private Long folderId;
    private String name;
    private int sortOrder;
}
