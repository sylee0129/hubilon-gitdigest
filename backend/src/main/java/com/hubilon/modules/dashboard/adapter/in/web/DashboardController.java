package com.hubilon.modules.dashboard.adapter.in.web;

import com.hubilon.common.response.Response;
import com.hubilon.modules.dashboard.application.port.in.GetDashboardSummaryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "대시보드 집계 API")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    @Operation(summary = "대시보드 요약 조회")
    @GetMapping("/summary")
    public Response<DashboardSummaryResponse> getSummary() {
        return Response.ok(DashboardSummaryResponse.from(getDashboardSummaryUseCase.getSummary()));
    }
}
