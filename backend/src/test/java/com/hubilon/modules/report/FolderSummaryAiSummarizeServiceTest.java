package com.hubilon.modules.report;

import com.hubilon.common.exception.custom.InvalidRequestException;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaEntity;
import com.hubilon.modules.folder.adapter.out.persistence.FolderJpaRepository;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.application.mapper.FolderSummaryAppMapper;
import com.hubilon.modules.report.application.service.FolderSummaryAiSummarizeService;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FolderAiSummaryResult;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.out.AiSummaryPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryQueryPort;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderSummaryAiSummarizeServiceTest {

    @Mock
    ProjectQueryPort projectQueryPort;

    @Mock
    ReportQueryPort reportQueryPort;

    @Mock
    AiSummaryPort aiSummaryPort;

    @Mock
    FolderSummaryQueryPort folderSummaryQueryPort;

    @Mock
    FolderSummaryCommandPort folderSummaryCommandPort;

    @Mock
    FolderSummaryAppMapper folderSummaryAppMapper;

    @Mock
    FolderJpaRepository folderJpaRepository;

    @InjectMocks
    FolderSummaryAiSummarizeService service;

    private final LocalDate START = LocalDate.of(2026, 4, 7);
    private final LocalDate END = LocalDate.of(2026, 4, 11);

    @Test
    void endDate가_startDate보다_이전이면_InvalidRequestException() {
        FolderSummaryAiSummarizeCommand command = new FolderSummaryAiSummarizeCommand(
                1L, END, START  // endDate < startDate
        );

        assertThatThrownBy(() -> service.summarize(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("종료일");
    }

    @Test
    void 같은_날짜도_유효하고_요약_반환() {
        // startDate == endDate → 허용
        LocalDate sameDay = LocalDate.of(2026, 4, 10);
        FolderSummaryAiSummarizeCommand command = new FolderSummaryAiSummarizeCommand(10L, sameDay, sameDay);

        Project project = Project.builder()
                .id(100L)
                .name("테스트 프로젝트")
                .gitlabUrl("http://gitlab.example.com")
                .accessToken("token")
                .authType(Project.AuthType.PAT)
                .gitlabProjectId(1L)
                .build();

        CommitInfo commit = CommitInfo.builder()
                .sha("abc123")
                .authorName("홍길동")
                .authorEmail("hong@example.com")
                .message("feat: 로그인 기능 구현")
                .build();

        Report report = Report.builder()
                .id(1L)
                .projectId(100L)
                .projectName("테스트 프로젝트")
                .startDate(sameDay)
                .endDate(sameDay)
                .commits(List.of(commit))
                .build();

        FolderSummary savedSummary = FolderSummary.builder()
                .id(1L)
                .folderId(10L)
                .folderName("개발팀")
                .startDate(sameDay)
                .endDate(sameDay)
                .totalCommitCount(1)
                .uniqueContributorCount(1)
                .summary("[금주 진행사항]")
                .progressSummary("[금주 진행사항]")
                .planSummary("[차주 진행계획]")
                .build();

        FolderSummaryResult expectedResult = new FolderSummaryResult(
                1L, 10L, "개발팀", sameDay, sameDay, 1, 1,
                "[금주 진행사항]", false, false,
                "[금주 진행사항]", "[차주 진행계획]", null, null
        );

        FolderJpaEntity folderEntity = mock(FolderJpaEntity.class);
        when(folderEntity.getName()).thenReturn("개발팀");

        when(projectQueryPort.findByFolderId(10L)).thenReturn(List.of(project));
        when(reportQueryPort.findByProjectIdsAndDateRange(eq(List.of(100L)), eq(sameDay), eq(sameDay)))
                .thenReturn(List.of(report));
        when(aiSummaryPort.summarizeFolder(any(), eq(sameDay), eq(sameDay), eq("개발팀")))
                .thenReturn(new FolderAiSummaryResult("[금주 진행사항]", "[차주 진행계획]", false));
        when(folderJpaRepository.findById(10L)).thenReturn(Optional.of(folderEntity));
        when(folderSummaryQueryPort.findByFolderIdAndDateRange(10L, sameDay, sameDay))
                .thenReturn(Optional.empty());
        when(folderSummaryCommandPort.save(any())).thenReturn(savedSummary);
        when(folderSummaryAppMapper.toResult(savedSummary)).thenReturn(expectedResult);

        FolderSummaryResult result = service.summarize(command);

        assertThat(result.progressSummary()).isEqualTo("[금주 진행사항]");
        assertThat(result.planSummary()).isEqualTo("[차주 진행계획]");
    }

    @Test
    void 빈폴더_프로젝트없음_예외없이_FolderSummary_생성() {
        FolderSummaryAiSummarizeCommand command = new FolderSummaryAiSummarizeCommand(10L, START, END);

        FolderSummary savedSummary = FolderSummary.builder()
                .id(1L)
                .folderId(10L)
                .folderName("빈팀")
                .startDate(START)
                .endDate(END)
                .totalCommitCount(0)
                .uniqueContributorCount(0)
                .summary("[금주 진행사항]")
                .progressSummary("[금주 진행사항]")
                .planSummary("[차주 진행계획]")
                .build();

        FolderSummaryResult expectedResult = new FolderSummaryResult(
                1L, 10L, "빈팀", START, END, 0, 0,
                "[금주 진행사항]", false, false,
                "[금주 진행사항]", "[차주 진행계획]", null, null
        );

        FolderJpaEntity folderEntity = mock(FolderJpaEntity.class);
        when(folderEntity.getName()).thenReturn("빈팀");

        when(projectQueryPort.findByFolderId(10L)).thenReturn(List.of());
        when(reportQueryPort.findByProjectIdsAndDateRange(eq(List.of()), eq(START), eq(END)))
                .thenReturn(List.of());
        when(aiSummaryPort.summarizeFolder(eq(List.of()), eq(START), eq(END), eq("빈팀")))
                .thenReturn(new FolderAiSummaryResult("[금주 진행사항]", "[차주 진행계획]", false));
        when(folderJpaRepository.findById(10L)).thenReturn(Optional.of(folderEntity));
        when(folderSummaryQueryPort.findByFolderIdAndDateRange(10L, START, END))
                .thenReturn(Optional.empty());
        when(folderSummaryCommandPort.save(any())).thenReturn(savedSummary);
        when(folderSummaryAppMapper.toResult(savedSummary)).thenReturn(expectedResult);

        FolderSummaryResult result = service.summarize(command);

        assertThat(result).isNotNull();
        assertThat(result.totalCommitCount()).isZero();
        assertThat(result.uniqueContributorCount()).isZero();
        assertThat(result.progressSummary()).isEqualTo("[금주 진행사항]");
        verify(folderSummaryCommandPort).save(any());
    }

    @Test
    void 정상케이스_두_섹션이_FolderSummaryResult에_포함되어_반환() {
        FolderSummaryAiSummarizeCommand command = new FolderSummaryAiSummarizeCommand(10L, START, END);

        Project project = Project.builder()
                .id(100L)
                .name("테스트 프로젝트")
                .gitlabUrl("http://gitlab.example.com")
                .accessToken("token")
                .authType(Project.AuthType.PAT)
                .gitlabProjectId(1L)
                .build();

        CommitInfo commit1 = CommitInfo.builder()
                .sha("abc123")
                .authorName("홍길동")
                .authorEmail("hong@example.com")
                .message("feat: 로그인 기능 구현")
                .build();

        CommitInfo commit2 = CommitInfo.builder()
                .sha("def456")
                .authorName("김철수")
                .authorEmail("kim@example.com")
                .message("fix: 버그 수정")
                .build();

        Report report = Report.builder()
                .id(1L)
                .projectId(100L)
                .projectName("테스트 프로젝트")
                .startDate(START)
                .endDate(END)
                .commits(List.of(commit1, commit2))
                .build();

        String progressSummary = "[금주 진행사항 (04/07~04/11)]\n- 홍길동: 로그인 기능 구현\n- 김철수: 버그 수정";
        String planSummary = "[차주 진행계획 (04/12~04/18)]\n- 홍길동: 로그아웃 기능 예정";

        FolderSummary savedSummary = FolderSummary.builder()
                .id(1L)
                .folderId(10L)
                .folderName("개발팀")
                .startDate(START)
                .endDate(END)
                .totalCommitCount(2)
                .uniqueContributorCount(2)
                .summary(progressSummary)
                .progressSummary(progressSummary)
                .planSummary(planSummary)
                .build();

        FolderSummaryResult expectedResult = new FolderSummaryResult(
                1L, 10L, "개발팀", START, END, 2, 2,
                progressSummary, false, false,
                progressSummary, planSummary, null, null
        );

        FolderJpaEntity folderEntity = mock(FolderJpaEntity.class);
        when(folderEntity.getName()).thenReturn("개발팀");

        when(projectQueryPort.findByFolderId(10L)).thenReturn(List.of(project));
        when(reportQueryPort.findByProjectIdsAndDateRange(eq(List.of(100L)), eq(START), eq(END)))
                .thenReturn(List.of(report));
        when(aiSummaryPort.summarizeFolder(any(), eq(START), eq(END), eq("개발팀")))
                .thenReturn(new FolderAiSummaryResult(progressSummary, planSummary, true));
        when(folderJpaRepository.findById(10L)).thenReturn(Optional.of(folderEntity));
        when(folderSummaryQueryPort.findByFolderIdAndDateRange(10L, START, END))
                .thenReturn(Optional.empty());
        when(folderSummaryCommandPort.save(any())).thenReturn(savedSummary);
        when(folderSummaryAppMapper.toResult(savedSummary)).thenReturn(expectedResult);

        FolderSummaryResult result = service.summarize(command);

        assertThat(result.progressSummary()).contains("[금주 진행사항");
        assertThat(result.planSummary()).contains("[차주 진행계획");
        verify(aiSummaryPort).summarizeFolder(any(), eq(START), eq(END), eq("개발팀"));
        verify(folderSummaryCommandPort).save(any());
    }
}
