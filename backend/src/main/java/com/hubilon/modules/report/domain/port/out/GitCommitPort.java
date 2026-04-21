package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.report.domain.model.CommitInfo;

import java.time.LocalDate;
import java.util.List;

public interface GitCommitPort {

    List<CommitInfo> fetchCommits(Long projectNumericId, String repoUrl, String accessToken, String authType,
                                   LocalDate startDate, LocalDate endDate);

    GitProvider supports();
}
