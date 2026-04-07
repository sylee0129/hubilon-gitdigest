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
import com.hubilon.modules.report.domain.port.out.GitLabPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
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
public class ReportAnalyzeService implements ReportAnalyzeUseCase {

    private final ProjectQueryPort projectQueryPort;
    private final ReportQueryPort reportQueryPort;
    private final ReportCommandPort reportCommandPort;
    private final GitLabPort gitLabPort;
    private final ReportAppMapper reportAppMapper;

    @Transactional
    @Override
    public List<ReportResult> analyze(ReportAnalyzeCommand command) {
        List<Project> projects = resolveProjects(command.projectId());

        return projects.stream()
                .map(project -> analyzeProject(project, command))
                .toList();
    }

    private List<Project> resolveProjects(Long projectId) {
        if (projectId != null) {
            Project project = projectQueryPort.findById(projectId)
                    .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + projectId));
            return List.of(project);
        }
        return projectQueryPort.findAll();
    }

    private ReportResult analyzeProject(Project project, ReportAnalyzeCommand command) {
        // 기존 보고서 캐시 확인 — 있으면 그대로 반환 (AI 요약은 버튼으로 별도 생성)
        Optional<Report> existing = reportQueryPort.findExisting(
                project.getId(), command.startDate(), command.endDate());

        if (existing.isPresent()) {
            log.info("Returning cached report for project={}", project.getId());
            return reportAppMapper.toResult(existing.get());
        }

        // GitLab에서 커밋 조회
        log.info("Fetching commits from GitLab for project={}, period={} ~ {}",
                project.getId(), command.startDate(), command.endDate());
        List<CommitInfo> commits = gitLabPort.fetchCommits(
                project.getGitlabProjectId(),
                project.getGitlabUrl(),
                project.getAccessToken(),
                project.getAuthType().name(),
                command.startDate(),
                command.endDate()
        );

        // DB 저장 (AI 요약은 없이 커밋만 저장)
        Report report = Report.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .aiSummary(null)
                .manuallyEdited(false)
                .commits(commits)
                .build();

        Report saved = reportCommandPort.save(report);
        return reportAppMapper.toResult(saved);
    }
}
