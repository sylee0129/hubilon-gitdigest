package com.hubilon.modules.dashboard.application.port.in;

import com.hubilon.modules.dashboard.application.dto.DashboardSummaryResult;

public interface GetDashboardSummaryUseCase {

    DashboardSummaryResult getSummary();
}
