package com.hubilon.modules.report;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.ReportAnalyzeCommand;
import com.hubilon.modules.report.application.dto.ReportResult;
import com.hubilon.modules.report.application.mapper.ReportAppMapper;
import com.hubilon.modules.report.application.service.ReportAnalyzeService;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.out.GitLabPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAnalyzeServiceTest {

    @Mock
    ProjectQueryPort projectQueryPort;

    @Mock
    ReportQueryPort reportQueryPort;

    @Mock
    ReportCommandPort reportCommandPort;

    @Mock
    GitLabPort gitLabPort;

    @Mock
    ReportAppMapper reportAppMapper;

    @InjectMocks
    ReportAnalyzeService reportAnalyzeService;

    private final LocalDate START = LocalDate.of(2026, 4, 1);
    private final LocalDate END = LocalDate.of(2026, 4, 7);

    @Test
    void projectIds_빈배열이면_빈리스트_반환() {
        // ReportController에서 빈 배열을 먼저 차단하지만, 서비스 내 resolveProjects 분기 검증
        // 빈 배열(not null)이면 stream이 빈 리스트를 생성 → analyze 결과도 빈 리스트
        ReportAnalyzeCommand command = new ReportAnalyzeCommand(null, Collections.emptyList(), START, END);

        List<ReportResult> results = reportAnalyzeService.analyze(command);

        assertThat(results).isEmpty();
        verifyNoInteractions(gitLabPort, reportCommandPort);
    }

    @Test
    void projectIds_단일_항목이면_해당_프로젝트만_분석() {
        Project project = Project.builder()
                .id(1L)
                .name("테스트 프로젝트")
                .gitlabUrl("http://gitlab.example.com")
                .accessToken("token")
                .authType(Project.AuthType.PAT)
                .gitlabProjectId(42L)
                .build();

        Report report = Report.builder()
                .id(10L)
                .projectId(1L)
                .projectName("테스트 프로젝트")
                .startDate(START)
                .endDate(END)
                .commits(List.of())
                .build();

        ReportResult result = new ReportResult(
                10L, 1L, "테스트 프로젝트", START, END, null, false, List.of(), 0, 0, null, false);

        when(projectQueryPort.findById(1L)).thenReturn(Optional.of(project));
        when(reportQueryPort.findExisting(eq(1L), eq(START), eq(END))).thenReturn(Optional.empty());
        when(gitLabPort.fetchCommits(anyLong(), anyString(), anyString(), anyString(), eq(START), eq(END)))
                .thenReturn(List.of());
        when(reportCommandPort.save(any())).thenReturn(report);
        when(reportAppMapper.toResult(report)).thenReturn(result);

        ReportAnalyzeCommand command = new ReportAnalyzeCommand(null, List.of(1L), START, END);
        List<ReportResult> results = reportAnalyzeService.analyze(command);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).projectId()).isEqualTo(1L);
        verify(projectQueryPort).findById(1L);
        verify(gitLabPort).fetchCommits(anyLong(), anyString(), anyString(), anyString(), eq(START), eq(END));
    }

    @Test
    void projectIds_존재하지않는_id이면_NotFoundException() {
        when(projectQueryPort.findById(999L)).thenReturn(Optional.empty());

        ReportAnalyzeCommand command = new ReportAnalyzeCommand(null, List.of(999L), START, END);

        assertThatThrownBy(() -> reportAnalyzeService.analyze(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void projectId_단일이면_해당_프로젝트만_분석() {
        Project project = Project.builder()
                .id(2L)
                .name("단일 프로젝트")
                .gitlabUrl("http://gitlab.example.com")
                .accessToken("token")
                .authType(Project.AuthType.PAT)
                .gitlabProjectId(55L)
                .build();

        Report report = Report.builder()
                .id(20L)
                .projectId(2L)
                .projectName("단일 프로젝트")
                .startDate(START)
                .endDate(END)
                .commits(List.of())
                .build();

        ReportResult result = new ReportResult(
                20L, 2L, "단일 프로젝트", START, END, null, false, List.of(), 0, 0, null, false);

        when(projectQueryPort.findById(2L)).thenReturn(Optional.of(project));
        when(reportQueryPort.findExisting(eq(2L), eq(START), eq(END))).thenReturn(Optional.empty());
        when(gitLabPort.fetchCommits(anyLong(), anyString(), anyString(), anyString(), eq(START), eq(END)))
                .thenReturn(List.of());
        when(reportCommandPort.save(any())).thenReturn(report);
        when(reportAppMapper.toResult(report)).thenReturn(result);

        ReportAnalyzeCommand command = new ReportAnalyzeCommand(2L, null, START, END);
        List<ReportResult> results = reportAnalyzeService.analyze(command);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).projectId()).isEqualTo(2L);
    }
}
