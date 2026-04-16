package com.hubilon.modules.user.adapter.out.persistence;

import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
import com.hubilon.modules.team.adapter.out.persistence.TeamRepository;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserCommandPort, UserQueryPort {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Override
    public User save(User user) {
        TeamJpaEntity teamEntity = null;
        if (user.getTeamId() != null) {
            teamEntity = teamRepository.findById(user.getTeamId()).orElse(null);
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(toJpaRole(user.getRole()))
                .team(teamEntity)
                .build();
        UserJpaEntity saved = userRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> findByQuery(String q) {
        return userRepository.findByQuery(q).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    private User toDomain(UserJpaEntity entity) {
        TeamJpaEntity team = entity.getTeam();
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : null)
                .role(toDomainRole(entity.getRole()))
                .build();
    }

    private UserJpaEntity.Role toJpaRole(User.Role role) {
        if (role == null) return UserJpaEntity.Role.USER;
        return switch (role) {
            case ADMIN -> UserJpaEntity.Role.ADMIN;
            case USER -> UserJpaEntity.Role.USER;
        };
    }

    private User.Role toDomainRole(UserJpaEntity.Role role) {
        if (role == null) return User.Role.USER;
        return switch (role) {
            case ADMIN -> User.Role.ADMIN;
            case USER -> User.Role.USER;
        };
    }
}
