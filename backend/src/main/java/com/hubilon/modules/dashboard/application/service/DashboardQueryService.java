package com.hubilon.modules.dashboard.application.service;

import com.hubilon.modules.dashboard.application.dto.DashboardSummaryResult;
import com.hubilon.modules.dashboard.application.port.in.GetDashboardSummaryUseCase;
import com.hubilon.modules.dashboard.application.port.out.DashboardQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService implements GetDashboardSummaryUseCase {

    private final DashboardQueryPort dashboardQueryPort;

    @Override
    public DashboardSummaryResult getSummary() {
        return dashboardQueryPort.query();
    }
}
