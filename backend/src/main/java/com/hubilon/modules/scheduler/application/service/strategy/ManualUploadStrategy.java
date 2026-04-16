package com.hubilon.modules.scheduler.application.service.strategy;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.folder.application.dto.FolderMemberResult;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.report.domain.model.FolderSummary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ManualUploadStrategy implements WeeklyReportUploadStrategy {

    @Override
    public WeeklyReportRowDto execute(FolderResult folder, FolderSummary summary, LocalDate startDate, LocalDate endDate) {
        List<String> members = folder.members() == null
                ? List.of()
                : folder.members().stream().map(FolderMemberResult::name).toList();

        return new WeeklyReportRowDto(
                folder.category().name(),
                folder.name(),
                members,
                summary.getProgressSummary(),
                summary.getPlanSummary()
        );
    }
}
