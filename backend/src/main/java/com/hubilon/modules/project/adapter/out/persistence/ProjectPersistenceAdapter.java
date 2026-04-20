package com.hubilon.modules.project.adapter.out.persistence;

import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    public List<Project> findAll(Long teamId) {
        return projectJpaRepository.findAllByTeamIdOrderBySortOrderAsc(teamId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void updateSortOrders(List<Long> orderedIds) {
        List<ProjectJpaEntity> entities = projectJpaRepository.findAllById(orderedIds);
        IntStream.range(0, orderedIds.size()).forEach(i -> {
            Long id = orderedIds.get(i);
            entities.stream()
                    .filter(e -> e.getId().equals(id))
                    .findFirst()
                    .ifPresent(e -> e.updateSortOrder(i));
        });
        projectJpaRepository.saveAll(entities);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Project> findByFolderId(Long folderId) {
        return projectJpaRepository.findByFolderIdOrderBySortOrderAsc(folderId).stream()
                .map(this::toDomain)
                .toList();
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
                .sortOrder(project.getSortOrder())
                .folderId(project.getFolderId())
                .teamId(project.getTeamId())
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
                .sortOrder(entity.getSortOrder())
                .folderId(entity.getFolderId())
                .teamId(entity.getTeamId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
