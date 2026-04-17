package com.hubilon.modules.folder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hubilon.modules.auth.adapter.out.jwt.JwtTokenAdapter;
import com.hubilon.modules.folder.adapter.in.web.FolderCreateRequest;
import com.hubilon.modules.folder.adapter.in.web.FolderReorderRequest;
import com.hubilon.modules.folder.adapter.in.web.FolderUpdateRequest;
import com.hubilon.modules.folder.domain.model.FolderStatus;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class FolderControllerTest {

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
    void 폴더_생성_성공() throws Exception {
        FolderCreateRequest req = new FolderCreateRequest("개발폴더", 1L, FolderStatus.IN_PROGRESS, List.of());

        mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("개발폴더"))
                .andExpect(jsonPath("$.data.categoryId").value(1));
    }

    @Test
    void 폴더_목록_조회() throws Exception {
        FolderCreateRequest req = new FolderCreateRequest("조회테스트폴더", 3L, FolderStatus.IN_PROGRESS, List.of());
        mockMvc.perform(post("/api/folders")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/folders")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 폴더_상태_필터_조회() throws Exception {
        mockMvc.perform(get("/api/folders")
                        .param("status", "IN_PROGRESS")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 폴더_수정_성공() throws Exception {
        FolderCreateRequest createReq = new FolderCreateRequest("수정전폴더", 3L, FolderStatus.IN_PROGRESS, List.of());
        String createBody = mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createBody).path("data").path("id").asLong();

        FolderUpdateRequest updateReq = new FolderUpdateRequest("수정후폴더", 2L, FolderStatus.COMPLETED, List.of());
        mockMvc.perform(put("/api/folders/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정후폴더"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void 폴더_삭제_성공() throws Exception {
        FolderCreateRequest req = new FolderCreateRequest("삭제테스트폴더", 3L, FolderStatus.IN_PROGRESS, List.of());
        String body = mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/folders/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void 폴더_순서변경_성공() throws Exception {
        FolderCreateRequest req1 = new FolderCreateRequest("폴더A", 1L, FolderStatus.IN_PROGRESS, List.of());
        FolderCreateRequest req2 = new FolderCreateRequest("폴더B", 1L, FolderStatus.IN_PROGRESS, List.of());

        String body1 = mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andReturn().getResponse().getContentAsString();
        String body2 = mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn().getResponse().getContentAsString();

        Long id1 = objectMapper.readTree(body1).path("data").path("id").asLong();
        Long id2 = objectMapper.readTree(body2).path("data").path("id").asLong();

        FolderReorderRequest reorderReq = new FolderReorderRequest(List.of(
                new FolderReorderRequest.OrderItem(id1, 2),
                new FolderReorderRequest.OrderItem(id2, 1)
        ));

        mockMvc.perform(patch("/api/folders/reorder")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 폴더_생성_이름_빈값_실패() throws Exception {
        FolderCreateRequest req = new FolderCreateRequest("", 1L, FolderStatus.IN_PROGRESS, List.of());

        mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증없이_폴더_조회_실패() throws Exception {
        mockMvc.perform(get("/api/folders"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was: " + status);
                    }
                });
    }
}
