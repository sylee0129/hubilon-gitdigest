package com.hubilon.modules.team.adapter.out.persistence;

import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TeamPersistenceAdapter implements TeamQueryPort {

    private final TeamRepository teamRepository;

    @Override
    public Optional<Team> findByName(String name) {
        return teamRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<Team> findAll() {
        return teamRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private Team toDomain(TeamJpaEntity entity) {
        return Team.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
