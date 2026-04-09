package com.hubilon.modules.workproject.adapter.out.persistence;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaEntity;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.folder.adapter.out.persistence.WorkProjectJpaEntity;
import com.hubilon.modules.folder.adapter.out.persistence.WorkProjectJpaRepository;
import com.hubilon.modules.workproject.application.dto.WorkProjectReorderCommand;
import com.hubilon.modules.workproject.domain.model.WorkProject;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectCommandPort;
import com.hubilon.modules.workproject.domain.port.out.WorkProjectQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkProjectPersistenceAdapter implements WorkProjectCommandPort, WorkProjectQueryPort {

    private final WorkProjectJpaRepository workProjectJpaRepository;
    private final FolderJpaRepository folderJpaRepository;

    @Override
    public WorkProject save(WorkProject wp) {
        FolderJpaEntity folder = folderJpaRepository.findById(wp.getFolderId())
                .orElseThrow(() -> new NotFoundException("폴더를 찾을 수 없습니다. id=" + wp.getFolderId()));
        WorkProjectJpaEntity entity = WorkProjectJpaEntity.builder()
                .id(wp.getId())
                .folder(folder)
                .name(wp.getName())
                .sortOrder(wp.getSortOrder())
                .build();
        WorkProjectJpaEntity saved = workProjectJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        workProjectJpaRepository.deleteById(id);
    }

    @Override
    public void updateSortOrders(List<WorkProjectReorderCommand.OrderItem> orders) {
        orders.forEach(o -> workProjectJpaRepository.findById(o.id())
                .ifPresent(e -> e.updateSortOrder(o.sortOrder())));
    }

    @Override
    public Optional<WorkProject> findById(Long id) {
        return workProjectJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return workProjectJpaRepository.existsById(id);
    }

    @Override
    public List<WorkProject> findByFolderId(Long folderId) {
        return workProjectJpaRepository.findByFolderIdOrderBySortOrderAsc(folderId).stream()
                .map(this::toDomain)
                .toList();
    }

    private WorkProject toDomain(WorkProjectJpaEntity e) {
        return WorkProject.builder()
                .id(e.getId())
                .folderId(e.getFolder().getId())
                .name(e.getName())
                .sortOrder(e.getSortOrder())
                .build();
    }
}
