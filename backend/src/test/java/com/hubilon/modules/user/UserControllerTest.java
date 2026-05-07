package com.hubilon.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hubilon.auth.UserContext;
import com.hubilon.auth.UserInfo;
import com.hubilon.config.TestSecurityConfig;
import com.hubilon.modules.user.adapter.in.web.UserRegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class UserControllerTest {

    @Autowired
    WebApplicationContext context;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

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
    void 사용자_등록_성공() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("홍길동", "hong@test.com", "pass1234!", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.email").value("hong@test.com"));
    }

    @Test
    void 사용자_전체_목록_조회() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("테스트유저", "testuser@test.com", "pass1234!", null);
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_이름_검색() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("검색대상유저", "searchme@test.com", "pass1234!", null);
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/users")
                        .param("q", "검색대상"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_삭제_성공() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("삭제유저", "delete@test.com", "pass1234!", null);
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void 사용자_삭제후_재조회_없음() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("삭제후재조회", "gone@test.com", "pass1234!", null);
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/users/" + id));

        mockMvc.perform(get("/api/users")
                        .param("q", "삭제후재조회"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_등록_이메일_형식_오류() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("잘못된이메일", "not-an-email", "pass1234!", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
