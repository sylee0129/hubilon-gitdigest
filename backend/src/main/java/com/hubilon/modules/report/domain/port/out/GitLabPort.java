package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.CommitInfo;

import java.time.LocalDate;
import java.util.List;

public interface GitLabPort {

    List<CommitInfo> fetchCommits(Long gitlabProjectId, String gitlabBaseUrl, String accessToken, String authType,
                                   LocalDate startDate, LocalDate endDate);
}
