package com.hubilon.modules.project.application.service.command;

import com.hubilon.common.exception.custom.NotFoundException;
import com.hubilon.modules.project.domain.model.Project;
import com.hubilon.modules.project.domain.port.in.ProjectDeleteUseCase;
import com.hubilon.modules.project.domain.port.out.ProjectCommandPort;
import com.hubilon.modules.project.domain.port.out.ProjectQueryPort;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import com.hubilon.modules.report.domain.port.out.ReportCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDeleteService implements ProjectDeleteUseCase {

    private final ProjectCommandPort projectCommandPort;
    private final ProjectQueryPort projectQueryPort;
    private final ReportCommandPort reportCommandPort;
    private final FolderSummaryCommandPort folderSummaryCommandPort;

    @Transactional
    @Override
    public void delete(Long id) {
        log.info("Deleting project id={}", id);
        Project project = projectQueryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("프로젝트를 찾을 수 없습니다. id=" + id));

        reportCommandPort.deleteByProjectId(id);

        if (project.getFolderId() != null) {
            folderSummaryCommandPort.deleteByFolderId(project.getFolderId());
        }

        projectCommandPort.deleteById(id);
    }
}
