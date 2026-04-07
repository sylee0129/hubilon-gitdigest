package com.hubilon.modules.project.adapter.out.persistence;

import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectCommandPort, ProjectQueryPort {

    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = toEntity(project);
        ProjectJpaEntity saved = projectJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        projectJpaRepository.deleteById(id);
    }

    @Override
    public List<Project> findAll() {
        return projectJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return projectJpaRepository.existsById(id);
    }

    private ProjectJpaEntity toEntity(Project project) {
        return ProjectJpaEntity.builder()
                .id(project.getId())
                .name(project.getName())
                .gitlabUrl(project.getGitlabUrl())
                .accessToken(project.getAccessToken())
                .authType(ProjectJpaEntity.AuthType.valueOf(project.getAuthType().name()))
                .gitlabProjectId(project.getGitlabProjectId())
                .build();
    }

    private Project toDomain(ProjectJpaEntity entity) {
        return Project.builder()
                .id(entity.getId())
                .name(entity.getName())
                .gitlabUrl(entity.getGitlabUrl())
                .accessToken(entity.getAccessToken())
                .authType(Project.AuthType.valueOf(entity.getAuthType().name()))
                .gitlabProjectId(entity.getGitlabProjectId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
