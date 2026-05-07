package com.hubilon.modules.dashboard;

import com.hubilon.auth.UserContext;
import com.hubilon.auth.UserInfo;
import com.hubilon.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class DashboardControllerTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        UserContext.set(new UserInfo("test-id", "테스트유저", "test@hubilon.com", List.of("ROLE_USER")));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void 대시보드_요약_조회_성공() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalFolderCount").isNumber())
                .andExpect(jsonPath("$.data.inProgressFolderCount").isNumber())
                .andExpect(jsonPath("$.data.todayCommitCount").isNumber())
                .andExpect(jsonPath("$.data.weeklyCommitCount").isNumber())
                .andExpect(jsonPath("$.data.recentActiveFolders").isArray());
    }
}
