package com.hubilon.modules.report.application.service;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.ReportAnalyzeCommand;
import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.mapper.ReportAppMapper;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.ReportAnalyzeUseCase;
import com.hubilon.modules.project.domain.model.GitProvider;
import com.hubilon.modules.report.domain.port.out.GitCommitPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportAnalyzeService implements ReportAnalyzeUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ReportQueryPort reportQueryPort;
    private final ReportCommandPort reportCommandPort;
    private final Map<GitProvider, GitCommitPort> commitPortMap;
    private final ReportAppMapper reportAppMapper;

    public ReportAnalyzeService(List<GitCommitPort> commitPorts,
                                 ProjectQueryPort projectQueryPort,
                                 ReportQueryPort reportQueryPort,
                                 ReportCommandPort reportCommandPort,
                                 ReportAppMapper reportAppMapper) {
        this.commitPortMap = commitPorts.stream()
                .collect(Collectors.toMap(GitCommitPort::supports, p -> p));
        this.projectQueryPort = projectQueryPort;
        this.reportQueryPort = reportQueryPort;
        this.reportCommandPort = reportCommandPort;
        this.reportAppMapper = reportAppMapper;
    }

    @Transactional
    @Override
    public List<ReportResult> analyze(ReportAnalyzeCommand command) {
        List<Project> projects = resolveProjects(command);

        return projects.stream()
                .map(project -> analyzeProject(project, command))
                .toList();
    }

    private List<Project> resolveProjects(ReportAnalyzeCommand command) {
        if (command.projectIds() != null && !command.projectIds().isEmpty()) {
            return command.projectIds().stream()
                    .map(id -> projectQueryPort.findById(id)
                            .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id)))
                    .toList();
        }
        if (command.projectId() != null) {
            Project project = projectQueryPort.findById(command.projectId())
                    .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + command.projectId()));
            return List.of(project);
        }
        if (command.teamId() != null) {
            return projectQueryPort.findAll(command.teamId());
        }
        return List.of();
    }

    private ReportResult analyzeProject(Project project, ReportAnalyzeCommand command) {
        // 진행 중인 주(endDate >= 오늘)이면 항상 최신 커밋 조회
        boolean isCurrentPeriod = !command.endDate().isBefore(LocalDate.now());

        if (!isCurrentPeriod) {
            // 완료된 주 — 캐시된 보고서 반환 (AI 요약은 버튼으로 별도 생성)
            Optional<Report> existing = reportQueryPort.findExisting(
                    project.getId(), command.startDate(), command.endDate());
            if (existing.isPresent()) {
                log.info("Returning cached report for project={}", project.getId());
                return reportAppMapper.toResult(existing.get());
            }
        }

        // Git provider에 따라 커밋 조회
        GitProvider provider = project.getGitProvider() != null ? project.getGitProvider() : GitProvider.GITLAB;
        GitCommitPort commitPort = commitPortMap.get(provider);
        if (commitPort == null) {
            throw new IllegalStateException("지원하지 않는 Git provider: " + provider);
        }
        log.info("Fetching commits from {} for project={}, period={} ~ {}",
                provider, project.getId(), command.startDate(), command.endDate());
        List<CommitInfo> commits = commitPort.fetchCommits(
                project.getGitlabProjectId(),
                project.getGitlabUrl(),
                project.getAccessToken(),
                project.getAuthType().name(),
                command.startDate(),
                command.endDate()
        );

        // 진행 중인 주 — 기존 레코드가 있으면 커밋만 갱신
        if (isCurrentPeriod) {
            Optional<Report> existing = reportQueryPort.findExisting(
                    project.getId(), command.startDate(), command.endDate());
            if (existing.isPresent()) {
                log.info("Refreshing commits for current-period report, project={}", project.getId());
                Report refreshed = existing.get().withRefreshedCommits(commits);
                Report saved = reportCommandPort.save(refreshed);
                return reportAppMapper.toResult(saved);
            }
        }

        // 신규 생성 (완료된 주 캐시 없음 or 진행 중인 주 첫 생성)
        Report report = Report.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .aiSummary(null)
                .manuallyEdited(false)
                .commits(commits)
                .build();

        try {
            Report saved = reportCommandPort.save(report);
            return reportAppMapper.toResult(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("Report 중복 저장 감지 (race condition), 기존 레코드 반환: projectId={}, period={}-{}",
                    project.getId(), command.startDate(), command.endDate());
            return reportQueryPort.findExisting(project.getId(), command.startDate(), command.endDate())
                    .map(reportAppMapper::toResult)
                    .orElseThrow(() -> new RuntimeException("Report not found after conflict", ex));
        }
    }
}
