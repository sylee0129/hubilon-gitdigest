package com.hubilon.modules.folder.adapter.out.persistence;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.category.adapter.out.persistence.CategoryJpaEntity;
import com.hubilon.modules.category.adapter.out.persistence.CategoryJpaRepository;
import com.hubilon.modules.folder.application.dto.FolderMemberResult;
import com.hubilon.modules.folder.application.dto.FolderReorderCommand;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.folder.application.dto.WorkProjectResult;
import com.hubilon.modules.folder.domain.model.Folder;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.folder.domain.port.out.FolderCommandPort;
import com.hubilon.modules.folder.domain.port.out.FolderQueryPort;
import com.hubilon.modules.user.adapter.out.persistence.UserRepository;
import com.hubilon.modules.user.domain.port.out.UserFolderMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FolderPersistenceAdapter implements FolderCommandPort, FolderQueryPort, UserFolderMemberPort {

    private final FolderJpaRepository folderJpaRepository;
    private final FolderMemberJpaRepository folderMemberJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Folder save(Folder folder, List<Long> memberIds) {
        CategoryJpaEntity categoryEntity = categoryJpaRepository.findById(folder.getCategoryId())
                .orElseThrow(() -> new NotFoundException("카테고리를 찾을 수 없습니다. id=" + folder.getCategoryId()));

        FolderJpaEntity entity;
        if (folder.getId() != null) {
            entity = folderJpaRepository.findById(folder.getId())
                    .orElse(null);
            if (entity != null) {
                entity.updateName(folder.getName());
                entity.updateCategory(categoryEntity);
                entity.updateStatus(folder.getStatus());
            } else {
                entity = FolderJpaEntity.builder()
                        .name(folder.getName())
                        .category(categoryEntity)
                        .status(folder.getStatus())
                        .sortOrder(folder.getSortOrder())
                        .build();
            }
        } else {
            entity = FolderJpaEntity.builder()
                    .name(folder.getName())
                    .category(categoryEntity)
                    .status(folder.getStatus())
                    .sortOrder(folder.getSortOrder())
                    .build();
        }
        FolderJpaEntity saved = folderJpaRepository.saveAndFlush(entity);

        // 멤버 동기화
        folderMemberJpaRepository.deleteByFolderId(saved.getId());
        folderMemberJpaRepository.flush();
        List<FolderMemberJpaEntity> members = memberIds.stream()
                .map(userId -> FolderMemberJpaEntity.builder().folder(saved).userId(userId).build())
                .toList();
        folderMemberJpaRepository.saveAll(members);

        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        folderJpaRepository.deleteById(id);
    }

    @Override
    public void updateSortOrders(List<FolderReorderCommand.FolderOrderItem> orders) {
        orders.forEach(o -> folderJpaRepository.findById(o.id())
                .ifPresent(e -> e.updateSortOrder(o.sortOrder())));
    }

    @Override
    public List<FolderResult> findAllWithDetails(FolderStatus status) {
        List<FolderJpaEntity> entities = status != null
                ? folderJpaRepository.findAllWithDetailsByStatus(status)
                : folderJpaRepository.findAllWithDetails();
        return entities.stream().map(this::toResult).toList();
    }

    @Override
    public Optional<FolderResult> findWithDetailsById(Long id) {
        return folderJpaRepository.findWithDetailsById(id).map(this::toResult);
    }

    @Override
    public boolean existsById(Long id) {
        return folderJpaRepository.existsById(id);
    }

    @Override
    public int countWorkProjectsByFolderId(Long folderId) {
        return folderJpaRepository.findById(folderId)
                .map(f -> f.getWorkProjects().size())
                .orElse(0);
    }

    // UserFolderMemberPort 구현
    @Override
    public List<UserFolderMemberPort.FolderRef> findFoldersByMemberId(Long userId) {
        return folderMemberJpaRepository.findByUserId(userId).stream()
                .map(m -> new UserFolderMemberPort.FolderRef(m.getFolder().getId(), m.getFolder().getName()))
                .toList();
    }

    private Folder toDomain(FolderJpaEntity entity) {
        return Folder.builder()
                .id(entity.getId())
                .name(entity.getName())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .status(entity.getStatus())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private FolderResult toResult(FolderJpaEntity entity) {
        List<FolderMemberResult> members = entity.getMembers().stream()
                .map(m -> userRepository.findById(m.getUserId())
                        .map(u -> new FolderMemberResult(u.getId(), u.getName(), u.getTeam() != null ? u.getTeam().getName() : null))
                        .orElse(null))
                .filter(m -> m != null)
                .toList();

        List<WorkProjectResult> workProjects = entity.getWorkProjects().stream()
                .sorted(Comparator.comparingInt(WorkProjectJpaEntity::getSortOrder))
                .map(wp -> new WorkProjectResult(wp.getId(), wp.getName(), wp.getSortOrder()))
                .toList();

        return new FolderResult(
                entity.getId(), entity.getName(),
                entity.getCategory().getId(), entity.getCategory().getName(),
                entity.getStatus(), entity.getSortOrder(), members, workProjects
        );
    }
}
