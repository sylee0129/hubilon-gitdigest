package com.hubilon.modules.project.application.service.query;

import com.hubilon.modules.project.application.dto.ProjectSearchResult;
import com.hubilon.modules.project.application.mapper.ProjectAppMapper;
import com.hubilon.modules.project.domain.port.in.ProjectSearchUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSearchService implements ProjectSearchUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ProjectAppMapper projectAppMapper;

    @Transactional(readOnly = true)
    @Override
    public List<ProjectSearchResult> searchAll(Long teamId) {
        return projectQueryPort.findAll(teamId).stream()
                .map(projectAppMapper::toSearchResult)
                .toList();
    }
}
