package com.hubilon.modules.dashboard;

import com.hubilon.modules.auth.adapter.out.jwt.JwtTokenAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JwtTokenAdapter jwtTokenAdapter;

    MockMvc mockMvc;
    String token;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        token = "Bearer " + jwtTokenAdapter.generateAccessToken("test@hubilon.com");
    }

    @Test
    void 대시보드_요약_조회_성공() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalFolderCount").isNumber())
                .andExpect(jsonPath("$.data.inProgressFolderCount").isNumber())
                .andExpect(jsonPath("$.data.todayCommitCount").isNumber())
                .andExpect(jsonPath("$.data.weeklyCommitCount").isNumber())
                .andExpect(jsonPath("$.data.recentActiveFolders").isArray());
    }

    @Test
    void 인증없이_대시보드_요약_조회_실패() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was: " + status);
                    }
                });
    }
}
