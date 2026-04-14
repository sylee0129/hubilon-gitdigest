package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.FolderSummaryAiPreviewResult;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.FolderSummaryAiPreviewUseCase;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderSummaryAiPreviewService implements FolderSummaryAiPreviewUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ReportQueryPort reportQueryPort;
    private final AiSummaryPort aiSummaryPort;
    private final FolderJpaRepository folderJpaRepository;

    @Override
    public FolderSummaryAiPreviewResult preview(FolderSummaryAiSummarizeCommand command) {
        if (command.startDate() == null || command.endDate() == null) {
            throw new InvalidRequestException("시작일/종료일은 필수입니다.");
        }
        if (command.endDate().isBefore(command.startDate())) {
            throw new InvalidRequestException("종료일은 시작일보다 이전일 수 없습니다.");
        }

        List<Project> projects = projectQueryPort.findByFolderId(command.folderId());
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        List<Report> reports = reportQueryPort.findByProjectIdsAndDateRange(
                projectIds, command.startDate(), command.endDate());

        List<CommitInfo> allCommits = reports.stream()
                .flatMap(r -> r.getCommits() == null ? java.util.stream.Stream.empty() : r.getCommits().stream())
                .toList();

        String folderName = folderJpaRepository.findById(command.folderId())
                .map(f -> f.getName())
                .orElse("폴더 #" + command.folderId());

        log.info("Generating folder AI preview (no save) for folderId={}, period={} ~ {}",
                command.folderId(), command.startDate(), command.endDate());

        FolderAiSummaryResult aiResult = aiSummaryPort.summarizeFolder(
                allCommits, command.startDate(), command.endDate(), folderName);

        return new FolderSummaryAiPreviewResult(
                aiResult.progressSummary(),
                aiResult.planSummary(),
                !aiResult.aiUsed()
        );
    }
}
