package com.hubilon.modules.scheduler.application.service.strategy;

import com.hubilon.modules.confluence.adapter.in.web.WeeklyConfluenceRequest.WeeklyReportRowDto;
import com.hubilon.modules.folder.application.dto.FolderResult;
import com.hubilon.modules.report.domain.model.FolderSummary;

import java.time.LocalDate;

public interface WeeklyReportUploadStrategy {

    /**
     * 폴더의 주간보고 row 데이터를 준비한다. (Confluence 업로드는 스케줄러가 일괄 처리)
     *
     * @param folder    대상 폴더 정보 (멤버 포함)
     * @param summary   FolderSummary (progressSummary, planSummary 포함)
     * @param startDate 보고 시작일
     * @param endDate   보고 종료일
     * @return Confluence 테이블에 들어갈 행 데이터
     */
    WeeklyReportRowDto execute(FolderResult folder, FolderSummary summary, LocalDate startDate, LocalDate endDate);
}
