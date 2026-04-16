package com.hubilon.modules.project;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.application.service.command.ProjectDeleteService;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectDeleteServiceTest {

    @Mock
    ProjectCommandPort projectCommandPort;

    @Mock
    ProjectQueryPort projectQueryPort;

    @Mock
    ReportCommandPort reportCommandPort;

    @Mock
    FolderSummaryCommandPort folderSummaryCommandPort;

    @InjectMocks
    ProjectDeleteService service;

    @Test
    void 존재하지않는_프로젝트_삭제시_NotFoundException() {
        when(projectQueryPort.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(reportCommandPort, folderSummaryCommandPort, projectCommandPort);
    }

    @Test
    void 폴더없는_프로젝트_삭제시_Report만_삭제_FolderSummary는_삭제안됨() {
        Project project = Project.builder()
                .id(1L)
                .name("테스트 프로젝트")
                .folderId(null)
                .build();

        when(projectQueryPort.findById(1L)).thenReturn(Optional.of(project));

        service.delete(1L);

        verify(reportCommandPort).deleteByProjectId(1L);
        verifyNoInteractions(folderSummaryCommandPort);
        verify(projectCommandPort).deleteById(1L);
    }

    @Test
    void 폴더있는_프로젝트_삭제시_Report와_FolderSummary_모두_삭제() {
        Project project = Project.builder()
                .id(2L)
                .name("폴더 있는 프로젝트")
                .folderId(10L)
                .build();

        when(projectQueryPort.findById(2L)).thenReturn(Optional.of(project));

        service.delete(2L);

        verify(reportCommandPort).deleteByProjectId(2L);
        verify(folderSummaryCommandPort).deleteByFolderId(10L);
        verify(projectCommandPort).deleteById(2L);
    }
}
