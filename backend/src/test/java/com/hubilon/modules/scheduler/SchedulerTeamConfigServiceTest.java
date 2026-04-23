package com.hubilon.modules.scheduler;

import com.hubilon.modules.scheduler.application.service.SchedulerTeamConfigService;
import com.hubilon.modules.scheduler.domain.model.SchedulerTeamConfig;
import com.hubilon.modules.scheduler.domain.port.out.SchedulerTeamConfigPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerTeamConfigServiceTest {

    @Mock SchedulerTeamConfigPort schedulerTeamConfigPort;
    @Mock TeamQueryPort teamQueryPort;

    @InjectMocks
    SchedulerTeamConfigService schedulerTeamConfigService;

    private static final Long TEAM_ID = 10L;
    private static final String TEAM_NAME = "개발팀";

    private Team team() {
        return Team.builder().id(TEAM_ID).name(TEAM_NAME).deptId(1L).build();
    }

    @Test
    void upsert_신규팀_create_후_저장() {
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));
        when(schedulerTeamConfigPort.findByTeamId(TEAM_ID)).thenReturn(Optional.empty());
        when(schedulerTeamConfigPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchedulerTeamConfig result = schedulerTeamConfigService.upsert(TEAM_ID, true);

        assertThat(result.teamId()).isEqualTo(TEAM_ID);
        assertThat(result.teamName()).isEqualTo(TEAM_NAME);
        assertThat(result.enabled()).isTrue();
        assertThat(result.id()).isNull(); // 신규 — DB 저장 전 id 없음
    }

    @Test
    void upsert_기존팀_enabled_false_업데이트() {
        SchedulerTeamConfig existing = new SchedulerTeamConfig(99L, TEAM_ID, TEAM_NAME, true);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));
        when(schedulerTeamConfigPort.findByTeamId(TEAM_ID)).thenReturn(Optional.of(existing));
        when(schedulerTeamConfigPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchedulerTeamConfig result = schedulerTeamConfigService.upsert(TEAM_ID, false);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.teamId()).isEqualTo(TEAM_ID);
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void upsert_존재하지_않는_팀_IllegalArgumentException() {
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulerTeamConfigService.upsert(TEAM_ID, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("팀을 찾을 수 없습니다");
    }

    @Test
    void upsert_기존팀_enabled_true_유지() {
        SchedulerTeamConfig existing = new SchedulerTeamConfig(5L, TEAM_ID, TEAM_NAME, false);
        when(teamQueryPort.findById(TEAM_ID)).thenReturn(Optional.of(team()));
        when(schedulerTeamConfigPort.findByTeamId(TEAM_ID)).thenReturn(Optional.of(existing));
        when(schedulerTeamConfigPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SchedulerTeamConfig result = schedulerTeamConfigService.upsert(TEAM_ID, true);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.enabled()).isTrue();
    }
}
