package com.hubilon.modules.confluence.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.confluence.application.port.in.UploadWeeklyReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Confluence", description = "Confluence 연동 API")
@RestController
@RequestMapping("/api/confluence")
@RequiredArgsConstructor
public class ConfluenceController {

    private final UploadWeeklyReportUseCase uploadWeeklyReportUseCase;

    @Operation(summary = "주간보고 Confluence 업로드",
            description = "주간보고 데이터를 Confluence 페이지로 생성하거나 수정합니다.")
    @PostMapping("/weekly-report")
    public Response<Map<String, String>> uploadWeeklyReport(
            @RequestBody WeeklyConfluenceRequest request) {
        String pageUrl = uploadWeeklyReportUseCase.upload(request);
        return Response.ok(Map.of("pageUrl", pageUrl));
    }
}
