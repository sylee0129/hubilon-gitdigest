package com.hubilon.modules.report.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.report.application.dto.ReportAnalyzeCommand;
import com.hubilon.modules.report.application.dto.ReportExportQuery;
import com.hubilon.modules.report.domain.port.in.ReportAiSummarizeUseCase;
import com.hubilon.modules.report.domain.port.in.ReportAnalyzeUseCase;
import com.hubilon.modules.report.domain.port.in.ReportExportUseCase;
import com.hubilon.modules.report.domain.port.in.ReportSummaryUpdateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Reports", description = "주간보고 분석 및 관리 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportAnalyzeUseCase reportAnalyzeUseCase;
    private final ReportAiSummarizeUseCase reportAiSummarizeUseCase;
    private final ReportSummaryUpdateUseCase reportSummaryUpdateUseCase;
    private final ReportExportUseCase reportExportUseCase;
    private final ReportWebMapper reportWebMapper;

    @Operation(summary = "주간 분석 결과 조회",
            description = "GitLab 커밋/변경파일 조회 후 AI 요약을 생성하여 반환합니다. 이미 DB에 있으면 캐시를 반환합니다.")
    @GetMapping
    public Response<List<ReportResponse>> analyze(
            @RequestParam(required = false) Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ReportAnalyzeCommand command = new ReportAnalyzeCommand(projectId, startDate, endDate);
        List<ReportResponse> responses = reportAnalyzeUseCase.analyze(command).stream()
                .map(reportWebMapper::toResponse)
                .toList();
        return Response.ok(responses);
    }

    @Operation(summary = "AI 요약 생성", description = "저장된 커밋을 기반으로 AI 요약을 생성합니다.")
    @PostMapping("/{reportId}/ai-summary")
    public Response<ReportResponse> generateAiSummary(@PathVariable Long reportId) {
        return Response.ok(
                reportWebMapper.toResponse(reportAiSummarizeUseCase.summarize(reportId))
        );
    }

    @Operation(summary = "요약 수동 편집", description = "보고서 요약을 수동으로 편집합니다. 이후 캐시 반환 시 수동 편집본이 우선됩니다.")
    @PutMapping("/{reportId}/summary")
    public Response<ReportResponse> updateSummary(
            @PathVariable Long reportId,
            @Valid @RequestBody ReportSummaryUpdateRequest request
    ) {
        return Response.ok(
                reportWebMapper.toResponse(
                        reportSummaryUpdateUseCase.updateSummary(reportId, reportWebMapper.toCommand(request))
                )
        );
    }

    @Operation(summary = "엑셀 내보내기", description = "주간보고 데이터를 xlsx 파일로 다운로드합니다.")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) List<Long> projectIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ReportExportQuery query = new ReportExportQuery(projectIds, startDate, endDate);
        byte[] excelBytes = reportExportUseCase.exportToExcel(query);

        String filename = "weekly-report-"
                + startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-"
                + endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
