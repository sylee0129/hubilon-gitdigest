package com.hubilon.modules.scheduler.adapter.out.persistence;

import com.hubilon.modules.scheduler.domain.model.SchedulerFolderResult;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogCommandPort;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerJobLogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SchedulerJobLogPersistenceAdapter implements SchedulerJobLogCommandPort, SchedulerJobLogQueryPort {

    private final SchedulerJobLogRepository jobLogRepository;

    @Override
    @Transactional
    public SchedulerJobLog save(SchedulerJobLog jobLog) {
        SchedulerJobLogJpaEntity entity;

        if (jobLog.getId() != null) {
            entity = jobLogRepository.findById(jobLog.getId())
                    .orElseGet(() -> toEntity(jobLog));
            entity.updateStatus(jobLog.getStatus(), jobLog.getSuccessCount(), jobLog.getFailCount());

            entity.getFolderResults().clear();
        } else {
            entity = toEntity(jobLog);
        }

        for (SchedulerFolderResult result : jobLog.getFolderResults()) {
            SchedulerFolderResultJpaEntity resultEntity = SchedulerFolderResultJpaEntity.builder()
                    .folderId(result.getFolderId())
                    .folderName(result.getFolderName())
                    .success(result.isSuccess())
                    .errorMessage(result.getErrorMessage())
                    .confluencePageUrl(result.getConfluencePageUrl())
                    .build();
            entity.addFolderResult(resultEntity);
        }

        SchedulerJobLogJpaEntity saved = jobLogRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchedulerJobLog> findAll(Pageable pageable) {
        return jobLogRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SchedulerJobLog> findById(Long id) {
        return jobLogRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByStatus(SchedulerJobStatus status) {
        return jobLogRepository.existsByStatus(status);
    }

    private SchedulerJobLogJpaEntity toEntity(SchedulerJobLog domain) {
        return SchedulerJobLogJpaEntity.builder()
                .id(domain.getId())
                .teamId(domain.getTeamId())
                .teamName(domain.getTeamName())
                .executedAt(domain.getExecutedAt())
                .status(domain.getStatus())
                .totalFolderCount(domain.getTotalFolderCount())
                .successCount(domain.getSuccessCount())
                .failCount(domain.getFailCount())
                .build();
    }

    private SchedulerJobLog toDomain(SchedulerJobLogJpaEntity entity) {
        List<SchedulerFolderResult> results = entity.getFolderResults().stream()
                .map(r -> SchedulerFolderResult.builder()
                        .id(r.getId())
                        .jobLogId(entity.getId())
                        .folderId(r.getFolderId())
                        .folderName(r.getFolderName())
                        .success(r.isSuccess())
                        .errorMessage(r.getErrorMessage())
                        .confluencePageUrl(r.getConfluencePageUrl())
                        .build())
                .toList();

        return SchedulerJobLog.builder()
                .id(entity.getId())
                .teamId(entity.getTeamId())
                .teamName(entity.getTeamName())
                .executedAt(entity.getExecutedAt())
                .status(entity.getStatus())
                .totalFolderCount(entity.getTotalFolderCount())
                .successCount(entity.getSuccessCount())
                .failCount(entity.getFailCount())
                .createdAt(entity.getCreatedAt())
                .folderResults(new java.util.ArrayList<>(results))
                .build();
    }
}
