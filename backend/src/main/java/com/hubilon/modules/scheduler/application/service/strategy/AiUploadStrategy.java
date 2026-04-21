package com.hubilon.modules.scheduler.application.service.strategy;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.folder.application.dto.FolderMemberResult;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.report.application.dto.FolderSummaryAiSummarizeCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryCreateCommand;
import com.hubilon.modules.report.application.dto.FolderSummaryResult;
import com.hubilon.modules.report.domain.model.FolderSummary;
import com.hubilon.modules.report.domain.port.in.FolderSummaryAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.in.FolderSummaryCreateUseCase;
import com.hubilon.modules.report.domain.port.out.FolderSummaryCommandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiUploadStrategy implements WeeklyReportUploadStrategy {

    private final FolderSummaryAiSummarizeUseCase folderSummaryAiSummarizeUseCase;
    private final FolderSummaryCreateUseCase folderSummaryCreateUseCase;
    private final FolderSummaryCommandPort folderSummaryCommandPort;

    @Override
    public WeeklyReportRowDto execute(FolderResult folder, FolderSummary summary, LocalDate startDate, LocalDate endDate) {
        FolderSummary effectiveSummary = resolveAiSummary(folder, summary, startDate, endDate);

        List<String> members = folder.members() == null
                ? List.of()
                : folder.members().stream().map(FolderMemberResult::name).toList();

        return new WeeklyReportRowDto(
                folder.categoryId(),
                folder.categoryName(),
                folder.name(),
                members,
                effectiveSummary.getProgressSummary(),
                effectiveSummary.getPlanSummary()
        );
    }

    private FolderSummary resolveAiSummary(FolderResult folder, FolderSummary existingSummary,
                                            LocalDate startDate, LocalDate endDate) {
        if (existingSummary == null) {
            // FolderSummary 없음 → Create 후 AI 요약
            folderSummaryCreateUseCase.create(new FolderSummaryCreateCommand(
                    folder.id(), startDate, endDate, null, null
            ));
            return runAiSummarize(folder.id(), startDate, endDate);
        }

        // FolderSummary 있으나 내용 없음 → AI 요약만
        return runAiSummarize(folder.id(), startDate, endDate);
    }

    private FolderSummary runAiSummarize(Long folderId, LocalDate startDate, LocalDate endDate) {
        FolderSummaryResult result;
        try {
            result = folderSummaryAiSummarizeUseCase.summarize(
                    new FolderSummaryAiSummarizeCommand(folderId, startDate, endDate)
            );
        } catch (Exception e) {
            log.error("AI 요약 실패 folderId={}, period={} ~ {}", folderId, startDate, endDate, e);
            // AI 요약 실패 시 빈 값으로 저장된 FolderSummary를 반환 (스킵 안 함)
            return FolderSummary.builder()
                    .folderId(folderId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .aiSummaryFailed(true)
                    .progressSummary("")
                    .planSummary("")
                    .build();
        }

        return FolderSummary.builder()
                .id(result.id())
                .folderId(result.folderId())
                .folderName(result.folderName())
                .startDate(result.startDate())
                .endDate(result.endDate())
                .totalCommitCount(result.totalCommitCount())
                .uniqueContributorCount(result.uniqueContributorCount())
                .summary(result.summary())
                .manuallyEdited(result.manuallyEdited())
                .aiSummaryFailed(result.aiSummaryFailed())
                .progressSummary(result.progressSummary())
                .planSummary(result.planSummary())
                .build();
    }
}
