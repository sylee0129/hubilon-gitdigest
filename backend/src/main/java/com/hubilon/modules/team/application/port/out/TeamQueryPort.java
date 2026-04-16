package com.hubilon.modules.team.application.port.out;

import com.hubilon.modules.team.domain.model.Team;

import java.util.List;
import java.util.Optional;

public interface TeamQueryPort {
    Optional<Team> findByName(String name);
    List<Team> findAll();
}
