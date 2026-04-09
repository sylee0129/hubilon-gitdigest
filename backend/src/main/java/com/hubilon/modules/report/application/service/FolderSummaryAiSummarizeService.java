package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.domain.model.AiSummaryResult;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.FolderSummaryAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderSummaryAiSummarizeService implements FolderSummaryAiSummarizeUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ReportQueryPort reportQueryPort;
    private final AiSummaryPort aiSummaryPort;
    private final FolderSummaryQueryPort folderSummaryQueryPort;
    private final FolderSummaryCommandPort folderSummaryCommandPort;
    private final FolderSummaryAppMapper folderSummaryAppMapper;
    private final FolderJpaRepository folderJpaRepository;

    @Transactional
    @Override
    public FolderSummaryResult summarize(FolderSummaryAiSummarizeCommand command) {
        // 1. folderId에 속한 프로젝트 조회
        List<Project> projects = projectQueryPort.findByFolderId(command.folderId());
        if (projects.isEmpty()) {
            throw new NotFoundException("폴더에 프로젝트가 없습니다. folderId=" + command.folderId());
        }

        // 2. 해당 기간의 모든 Report 조회
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        List<Report> reports = reportQueryPort.findByProjectIdsAndDateRange(
                projectIds, command.startDate(), command.endDate());

        // 3. 커밋 메시지 통합
        List<CommitInfo> allCommits = reports.stream()
                .flatMap(r -> r.getCommits() == null ? java.util.stream.Stream.empty() : r.getCommits().stream())
                .toList();

        int totalCommitCount = allCommits.size();
        int uniqueContributorCount = (int) allCommits.stream()
                .map(CommitInfo::getAuthorEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .count();

        // 4. AI 요약 생성
        log.info("Generating folder AI summary for folderId={}, period={} ~ {}",
                command.folderId(), command.startDate(), command.endDate());
        AiSummaryResult aiResult = aiSummaryPort.summarize(allCommits);

        // 5. 폴더명 조회
        String folderName = folderJpaRepository.findById(command.folderId())
                .map(f -> f.getName())
                .orElse("폴더 #" + command.folderId());

        // 6. 기존 FolderSummary 조회 후 저장/업데이트
        Optional<FolderSummary> existing = folderSummaryQueryPort.findByFolderIdAndDateRange(
                command.folderId(), command.startDate(), command.endDate());

        FolderSummary toSave;
        if (existing.isPresent()) {
            FolderSummary current = existing.get();
            toSave = FolderSummary.builder()
                    .id(current.getId())
                    .folderId(command.folderId())
                    .folderName(folderName)
                    .startDate(command.startDate())
                    .endDate(command.endDate())
                    .totalCommitCount(totalCommitCount)
                    .uniqueContributorCount(uniqueContributorCount)
                    .summary(aiResult.summary())
                    .manuallyEdited(false)
                    .aiSummaryFailed(!aiResult.aiUsed())
                    .createdAt(current.getCreatedAt())
                    .build();
        } else {
            toSave = FolderSummary.builder()
                    .folderId(command.folderId())
                    .folderName(folderName)
                    .startDate(command.startDate())
                    .endDate(command.endDate())
                    .totalCommitCount(totalCommitCount)
                    .uniqueContributorCount(uniqueContributorCount)
                    .summary(aiResult.summary())
                    .manuallyEdited(false)
                    .aiSummaryFailed(!aiResult.aiUsed())
                    .build();
        }

        FolderSummary saved = folderSummaryCommandPort.save(toSave);
        return folderSummaryAppMapper.toResult(saved);
    }
}
