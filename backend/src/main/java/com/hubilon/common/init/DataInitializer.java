package com.hubilon.common.init;

import com.hubilon.modules.team.adapter.out.persistence.TeamJpaEntity;
import com.hubilon.modules.team.adapter.out.persistence.TeamRepository;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserCommandPort userCommandPort;
    private final UserQueryPort userQueryPort;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final TeamRepository teamRepository;

    private static final String DEFAULT_PASSWORD = "hubilon1!";
    private static final String DEFAULT_TEAM_NAME = "플랫폼개발팀";

    private static final List<String[]> INIT_ADMINS = List.of(
            new String[]{"이광호", "khlee@hubilon.com"},
            new String[]{"이건",   "geonlee@hubilon.com"},
            new String[]{"지영관", "mouseyk@hubilon.com"},
            new String[]{"강보경", "bokyeong0113@hubilon.com"},
            new String[]{"최지수", "gsuchoi@hubilon.com"},
            new String[]{"이수연", "suyeon129@hubilon.com"},
            new String[]{"김미진", "mijinkim@hubilon.com"}
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. 팀 생성
        TeamJpaEntity team = teamRepository.findByName(DEFAULT_TEAM_NAME)
                .orElseGet(() -> {
                    TeamJpaEntity newTeam = TeamJpaEntity.builder()
                            .name(DEFAULT_TEAM_NAME)
                            .build();
                    TeamJpaEntity saved = teamRepository.save(newTeam);
                    log.info("기본 팀 생성: {}", DEFAULT_TEAM_NAME);
                    return saved;
                });

        // 2. 기존 사용자 team_id 업데이트
        int count = jdbcTemplate.update(
                "UPDATE users SET team_id = ? WHERE team_id IS NULL",
                team.getId()
        );
        log.info("초기 사용자 팀 정보 업데이트 완료: {}건", count);

        // 3. 초기 관리자 계정 생성
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        for (String[] admin : INIT_ADMINS) {
            String name = admin[0];
            String email = admin[1];

            if (userQueryPort.existsByEmail(email)) {
                continue;
            }

            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(encodedPassword)
                    .teamId(team.getId())
                    .role(User.Role.ADMIN)
                    .build();

            userCommandPort.save(user);
            log.info("초기 관리자 계정 생성: {} ({})", name, email);
        }
    }
}
