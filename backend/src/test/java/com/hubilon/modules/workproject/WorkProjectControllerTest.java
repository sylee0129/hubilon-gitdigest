package com.hubilon.modules.workproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hubilon.modules.auth.adapter.out.jwt.JwtTokenAdapter;
import com.hubilon.modules.folder.adapter.in.web.FolderCreateRequest;
import com.hubilon.modules.folder.domain.model.FolderStatus;
import com.hubilon.modules.workproject.adapter.in.web.WorkProjectCreateRequest;
import com.hubilon.modules.workproject.adapter.in.web.WorkProjectReorderRequest;
import com.hubilon.modules.workproject.adapter.in.web.WorkProjectUpdateRequest;
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
class WorkProjectControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JwtTokenAdapter jwtTokenAdapter;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

    MockMvc mockMvc;
    String token;
    Long folderId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        token = "Bearer " + jwtTokenAdapter.generateAccessToken("test@hubilon.com");

        FolderCreateRequest folderReq = new FolderCreateRequest("테스트폴더", 1L, FolderStatus.IN_PROGRESS, List.of(), null);
        String folderBody = mockMvc.perform(post("/api/folders")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderReq)))
                .andReturn().getResponse().getContentAsString();
        folderId = objectMapper.readTree(folderBody).path("data").path("id").asLong();
    }

    @Test
    void 세부프로젝트_생성_성공() throws Exception {
        WorkProjectCreateRequest req = new WorkProjectCreateRequest(folderId, "세부프로젝트A");

        mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("세부프로젝트A"))
                .andExpect(jsonPath("$.data.folderId").value(folderId));
    }

    @Test
    void 세부프로젝트_수정_성공() throws Exception {
        WorkProjectCreateRequest createReq = new WorkProjectCreateRequest(folderId, "수정전프로젝트");
        String body = mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        WorkProjectUpdateRequest updateReq = new WorkProjectUpdateRequest(folderId, "수정후프로젝트");
        mockMvc.perform(put("/api/work-projects/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정후프로젝트"));
    }

    @Test
    void 세부프로젝트_삭제_성공() throws Exception {
        WorkProjectCreateRequest req = new WorkProjectCreateRequest(folderId, "삭제프로젝트");
        String body = mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/work-projects/" + id)
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    void 세부프로젝트_순서변경_성공() throws Exception {
        WorkProjectCreateRequest req1 = new WorkProjectCreateRequest(folderId, "프로젝트1");
        WorkProjectCreateRequest req2 = new WorkProjectCreateRequest(folderId, "프로젝트2");

        String b1 = mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andReturn().getResponse().getContentAsString();
        String b2 = mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn().getResponse().getContentAsString();

        Long id1 = objectMapper.readTree(b1).path("data").path("id").asLong();
        Long id2 = objectMapper.readTree(b2).path("data").path("id").asLong();

        WorkProjectReorderRequest reorderReq = new WorkProjectReorderRequest(folderId, List.of(
                new WorkProjectReorderRequest.OrderItem(id1, 2),
                new WorkProjectReorderRequest.OrderItem(id2, 1)
        ));

        mockMvc.perform(patch("/api/work-projects/reorder")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reorderReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 세부프로젝트_이름_빈값_실패() throws Exception {
        WorkProjectCreateRequest req = new WorkProjectCreateRequest(folderId, "");

        mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 세부프로젝트_폴더아이디_없음_실패() throws Exception {
        WorkProjectCreateRequest req = new WorkProjectCreateRequest(null, "폴더없음프로젝트");

        mockMvc.perform(post("/api/work-projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증없이_세부프로젝트_생성_실패() throws Exception {
        WorkProjectCreateRequest req = new WorkProjectCreateRequest(folderId, "인증없음");

        mockMvc.perform(post("/api/work-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403 but was: " + status);
                    }
                });
    }
}
