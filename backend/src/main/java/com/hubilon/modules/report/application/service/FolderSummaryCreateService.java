package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.FolderSummaryCreateCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.FolderSummaryCreateUseCase;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderSummaryCreateService implements FolderSummaryCreateUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ReportQueryPort reportQueryPort;
    private final FolderSummaryCommandPort folderSummaryCommandPort;
    private final FolderSummaryAppMapper folderSummaryAppMapper;
    private final FolderJpaRepository folderJpaRepository;

    @Transactional
    @Override
    public FolderSummaryResult create(FolderSummaryCreateCommand command) {
        if (command.startDate() == null || command.endDate() == null) {
            throw new InvalidRequestException("시작일/종료일은 필수입니다.");
        }

        List<Project> projects = projectQueryPort.findByFolderId(command.folderId());
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        List<Report> reports = reportQueryPort.findByProjectIdsAndDateRange(
                projectIds, command.startDate(), command.endDate());

        List<CommitInfo> allCommits = reports.stream()
                .flatMap(r -> r.getCommits() == null ? java.util.stream.Stream.empty() : r.getCommits().stream())
                .toList();

        int totalCommitCount = allCommits.size();
        int uniqueContributorCount = (int) allCommits.stream()
                .map(CommitInfo::getAuthorEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .count();

        String folderName = folderJpaRepository.findById(command.folderId())
                .map(f -> f.getName())
                .orElse("폴더 #" + command.folderId());

        FolderSummary toSave = FolderSummary.builder()
                .folderId(command.folderId())
                .folderName(folderName)
                .startDate(command.startDate())
                .endDate(command.endDate())
                .totalCommitCount(totalCommitCount)
                .uniqueContributorCount(uniqueContributorCount)
                .summary(command.progressSummary())
                .manuallyEdited(true)
                .aiSummaryFailed(false)
                .progressSummary(command.progressSummary())
                .planSummary(command.planSummary())
                .build();

        FolderSummary saved = folderSummaryCommandPort.save(toSave);
        return folderSummaryAppMapper.toResult(saved);
    }
}
