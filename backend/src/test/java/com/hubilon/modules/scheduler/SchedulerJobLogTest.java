package com.hubilon.modules.scheduler;

import com.hubilon.modules.scheduler.domain.model.SchedulerJobLog;
import com.hubilon.modules.scheduler.domain.model.SchedulerJobStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerJobLogTest {

    private static final Long TEAM_ID = 1L;
    private static final String TEAM_NAME = "개발팀";

    @Test
    void createRunning_필드_초기값_검증() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 5);

        assertThat(log.getTeamId()).isEqualTo(TEAM_ID);
        assertThat(log.getTeamName()).isEqualTo(TEAM_NAME);
        assertThat(log.getStatus()).isEqualTo(SchedulerJobStatus.RUNNING);
        assertThat(log.getTotalFolderCount()).isEqualTo(5);
        assertThat(log.getSuccessCount()).isEqualTo(0);
        assertThat(log.getFailCount()).isEqualTo(0);
        assertThat(log.getExecutedAt()).isNotNull();
        assertThat(log.getFolderResults()).isEmpty();
    }

    @Test
    void recordSuccess_successCount_증가() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);

        log.recordSuccess(10L, "폴더A", "https://confluence.example.com/page");

        assertThat(log.getSuccessCount()).isEqualTo(1);
        assertThat(log.getFailCount()).isEqualTo(0);
        assertThat(log.getFolderResults()).hasSize(1);
        assertThat(log.getFolderResults().get(0).getFolderName()).isEqualTo("폴더A");
    }

    @Test
    void recordFail_failCount_증가() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);

        log.recordFail(20L, "폴더B", "AI 처리 실패");

        assertThat(log.getSuccessCount()).isEqualTo(0);
        assertThat(log.getFailCount()).isEqualTo(1);
        assertThat(log.getFolderResults()).hasSize(1);
        assertThat(log.getFolderResults().get(0).getErrorMessage()).isEqualTo("AI 처리 실패");
    }

    @Test
    void finalizeStatus_전부_성공이면_SUCCESS() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);
        log.recordSuccess(1L, "폴더A", null);
        log.recordSuccess(2L, "폴더B", null);

        log.finalizeStatus();

        assertThat(log.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
    }

    @Test
    void finalizeStatus_전부_실패이면_FAIL() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);
        log.recordFail(1L, "폴더A", "에러");
        log.recordFail(2L, "폴더B", "에러");

        log.finalizeStatus();

        assertThat(log.getStatus()).isEqualTo(SchedulerJobStatus.FAIL);
    }

    @Test
    void finalizeStatus_일부_성공_일부_실패이면_PARTIAL_FAIL() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);
        log.recordSuccess(1L, "폴더A", null);
        log.recordFail(2L, "폴더B", "에러");

        log.finalizeStatus();

        assertThat(log.getStatus()).isEqualTo(SchedulerJobStatus.PARTIAL_FAIL);
    }

    @Test
    void markSuccessAsFailed_성공항목을_실패로_전환() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 2);
        log.recordSuccess(1L, "폴더A", null);
        log.recordSuccess(2L, "폴더B", null);

        log.markSuccessAsFailed("Confluence 연결 실패");

        assertThat(log.getSuccessCount()).isEqualTo(0);
        assertThat(log.getFailCount()).isEqualTo(2);
        assertThat(log.getFolderResults())
                .allMatch(r -> r.getErrorMessage().contains("Confluence 업로드 실패"));
    }

    @Test
    void createRunning_totalFolderCount_0이면_SUCCESS_finalize() {
        SchedulerJobLog log = SchedulerJobLog.createRunning(TEAM_ID, TEAM_NAME, 0);

        log.finalizeStatus();

        assertThat(log.getStatus()).isEqualTo(SchedulerJobStatus.SUCCESS);
    }
}
