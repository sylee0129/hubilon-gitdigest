package com.hubilon.modules.team.application.port.out;

import com.hubilon.modules.team.domain.model.Team;

public interface TeamCommandPort {
    Team save(Team team);
}
