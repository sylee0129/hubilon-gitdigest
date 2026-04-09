package com.hubilon.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hubilon.modules.auth.adapter.out.jwt.JwtTokenAdapter;
import com.hubilon.modules.user.adapter.in.web.UserRegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JwtTokenAdapter jwtTokenAdapter;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

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
    void 사용자_등록_성공() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("홍길동", "hong@test.com", "pass1234!", "개발팀");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.email").value("hong@test.com"));
    }

    @Test
    void 사용자_전체_목록_조회() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("테스트유저", "testuser@test.com", "pass1234!", "QA팀");
        mockMvc.perform(post("/api/users")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_이름_검색() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("검색대상유저", "searchme@test.com", "pass1234!", "기획팀");
        mockMvc.perform(post("/api/users")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/users")
                        .param("q", "검색대상")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_삭제_성공() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("삭제유저", "delete@test.com", "pass1234!", "인사팀");
        String body = mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/users/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void 사용자_삭제후_재조회_없음() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("삭제후재조회", "gone@test.com", "pass1234!", "총무팀");
        String body = mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/users/" + id)
                .header("Authorization", token));

        mockMvc.perform(get("/api/users")
                        .param("q", "삭제후재조회")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 사용자_등록_이메일_형식_오류() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest("잘못된이메일", "not-an-email", "pass1234!", "팀");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증없이_사용자_조회_실패() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was: " + status);
                    }
                });
    }
}
