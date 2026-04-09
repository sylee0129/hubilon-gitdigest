package com.hubilon.common.init;

import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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

    private static final String DEFAULT_PASSWORD = "hubilon1!";

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
                    .department("")
                    .role(User.Role.ADMIN)
                    .build();

            userCommandPort.save(user);
            log.info("초기 관리자 계정 생성: {} ({})", name, email);
        }
    }
}
