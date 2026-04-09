package com.hubilon.modules.report.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.report.application.dto.FolderSummaryQuery;
import com.hubilon.modules.report.domain.port.in.FolderSummaryAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.in.FolderSummaryQueryUseCase;
import com.hubilon.modules.report.domain.port.in.FolderSummaryUpdateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Tag(name = "Folder Summary", description = "폴더 단위 통합 보고서 API")
@RestController
@RequestMapping("/api/reports/folder-summary")
@RequiredArgsConstructor
public class FolderSummaryController {

    private final FolderSummaryQueryUseCase folderSummaryQueryUseCase;
    private final FolderSummaryAiSummarizeUseCase folderSummaryAiSummarizeUseCase;
    private final FolderSummaryUpdateUseCase folderSummaryUpdateUseCase;
    private final FolderSummaryWebMapper folderSummaryWebMapper;

    @Operation(summary = "폴더 요약 조회", description = "지정 폴더 + 기간의 통합 보고서를 조회합니다.")
    @GetMapping
    public Response<FolderSummaryResponse> query(
            @RequestParam Long folderId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        FolderSummaryQuery query = new FolderSummaryQuery(folderId, startDate, endDate);
        Optional<FolderSummaryResponse> response = folderSummaryQueryUseCase.query(query)
                .map(folderSummaryWebMapper::toResponse);
        return Response.ok(response.orElse(null));
    }

    @Operation(summary = "AI 요약 생성", description = "폴더 내 모든 프로젝트의 커밋을 통합하여 AI 요약을 생성합니다.")
    @PostMapping("/ai-summary")
    public Response<FolderSummaryResponse> generateAiSummary(
            @Valid @RequestBody FolderSummaryAiSummarizeRequest request
    ) {
        return Response.ok(
                folderSummaryWebMapper.toResponse(
                        folderSummaryAiSummarizeUseCase.summarize(folderSummaryWebMapper.toCommand(request))
                )
        );
    }

    @Operation(summary = "요약 수동 편집", description = "폴더 요약을 수동으로 편집합니다. 폴더 멤버만 편집 가능합니다.")
    @PutMapping("/{id}/summary")
    public Response<FolderSummaryResponse> updateSummary(
            @PathVariable Long id,
            @Valid @RequestBody FolderSummaryUpdateRequest request,
            @AuthenticationPrincipal String currentUserEmail
    ) {
        return Response.ok(
                folderSummaryWebMapper.toResponse(
                        folderSummaryUpdateUseCase.update(id, folderSummaryWebMapper.toCommand(request), currentUserEmail)
                )
        );
    }
}
