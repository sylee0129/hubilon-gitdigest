package com.hubilon.modules.report.adapter.out.persistence;

import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FolderSummaryPersistenceAdapter implements FolderSummaryCommandPort, FolderSummaryQueryPort {

    private final FolderSummaryJpaRepository folderSummaryJpaRepository;

    @Override
    public FolderSummary save(FolderSummary folderSummary) {
        FolderSummaryJpaEntity entity;
        if (folderSummary.getId() != null) {
            entity = folderSummaryJpaRepository.findById(folderSummary.getId())
                    .orElse(buildEntity(folderSummary));
            entity.updateSummary(
                    folderSummary.getSummary(),
                    folderSummary.isManuallyEdited(),
                    folderSummary.isAiSummaryFailed(),
                    folderSummary.getTotalCommitCount(),
                    folderSummary.getUniqueContributorCount()
            );
        } else {
            entity = buildEntity(folderSummary);
        }
        FolderSummaryJpaEntity saved = folderSummaryJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FolderSummary> findById(Long id) {
        return folderSummaryJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<FolderSummary> findByFolderIdAndDateRange(Long folderId, LocalDate startDate, LocalDate endDate) {
        return folderSummaryJpaRepository.findByFolderIdAndDateRange(folderId, startDate, endDate)
                .map(this::toDomain);
    }

    private FolderSummaryJpaEntity buildEntity(FolderSummary folderSummary) {
        return FolderSummaryJpaEntity.builder()
                .id(folderSummary.getId())
                .folderId(folderSummary.getFolderId())
                .folderName(folderSummary.getFolderName())
                .startDate(folderSummary.getStartDate())
                .endDate(folderSummary.getEndDate())
                .totalCommitCount(folderSummary.getTotalCommitCount())
                .uniqueContributorCount(folderSummary.getUniqueContributorCount())
                .summary(folderSummary.getSummary())
                .manuallyEdited(folderSummary.isManuallyEdited())
                .aiSummaryFailed(folderSummary.isAiSummaryFailed())
                .build();
    }

    private FolderSummary toDomain(FolderSummaryJpaEntity entity) {
        return FolderSummary.builder()
                .id(entity.getId())
                .folderId(entity.getFolderId())
                .folderName(entity.getFolderName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalCommitCount(entity.getTotalCommitCount())
                .uniqueContributorCount(entity.getUniqueContributorCount())
                .summary(entity.getSummary())
                .manuallyEdited(entity.isManuallyEdited())
                .aiSummaryFailed(entity.isAiSummaryFailed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
