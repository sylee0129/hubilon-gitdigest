package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;
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
        // 입력 검증
        if (command.startDate() == null || command.endDate() == null) {
            throw new InvalidRequestException("시작일/종료일은 필수입니다.");
        }
        if (command.endDate().isBefore(command.startDate())) {
            throw new InvalidRequestException("종료일은 시작일보다 이전일 수 없습니다.");
        }

        // 1. folderId에 속한 프로젝트 조회
        List<Project> projects = projectQueryPort.findByFolderId(command.folderId());

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

        // 4. 폴더명 조회
        String folderName = folderJpaRepository.findById(command.folderId())
                .map(f -> f.getName())
                .orElse("폴더 #" + command.folderId());

        // 5. AI 요약 생성
        log.info("[FolderSummary] AI 요약 요청: folderId={}, folderName={}, projectCount={}, commitCount={}, period={} ~ {}",
                command.folderId(), folderName, projects.size(), allCommits.size(), command.startDate(), command.endDate());
        FolderAiSummaryResult aiResult = aiSummaryPort.summarizeFolder(
                allCommits, command.startDate(), command.endDate(), folderName);

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
                    .summary(aiResult.progressSummary())
                    .manuallyEdited(false)
                    .aiSummaryFailed(!aiResult.aiUsed())
                    .progressSummary(aiResult.progressSummary())
                    .planSummary(aiResult.planSummary())
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
                    .summary(aiResult.progressSummary())
                    .manuallyEdited(false)
                    .aiSummaryFailed(!aiResult.aiUsed())
                    .progressSummary(aiResult.progressSummary())
                    .planSummary(aiResult.planSummary())
                    .build();
        }

        FolderSummary saved = folderSummaryCommandPort.save(toSave);
        log.info("[FolderSummary] 저장 완료: folderId={}, aiSummaryFailed={}, progressSummary 길이={}",
                command.folderId(), saved.isAiSummaryFailed(),
                saved.getProgressSummary() == null ? 0 : saved.getProgressSummary().length());
        return folderSummaryAppMapper.toResult(saved);
    }
}
