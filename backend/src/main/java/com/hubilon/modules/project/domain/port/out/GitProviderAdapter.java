package com.hubilon.modules.project.domain.port.out;

import com.hubilon.modules.project.domain.model.GitProvider;

public interface GitProviderAdapter {
    Long resolveProjectId(String repoUrl, String token);
    String resolveProjectName(String repoUrl, String token);
    GitProvider supports();
}
