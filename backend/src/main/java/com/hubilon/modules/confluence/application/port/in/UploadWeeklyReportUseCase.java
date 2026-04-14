package com.hubilon.modules.confluence.application.port.in;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest;

public interface UploadWeeklyReportUseCase {

    /**
     * 주간보고 데이터를 Confluence 페이지로 업로드한다.
     *
     * @param request 주간보고 행 데이터 및 날짜 범위
     * @return 생성/수정된 Confluence 페이지 URL
     */
    String upload(WeeklyConfluenceRequest request);
}
