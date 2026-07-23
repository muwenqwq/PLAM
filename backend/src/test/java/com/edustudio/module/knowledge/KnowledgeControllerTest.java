package com.edustudio.module.knowledge;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.knowledge.dto.KnowledgeFileCreateRequest;
import com.edustudio.module.knowledge.dto.KnowledgeSearchRequest;
import com.edustudio.module.knowledge.service.KnowledgeService;
import com.edustudio.module.knowledge.vo.KnowledgeFileVO;
import com.edustudio.module.knowledge.vo.KnowledgeSearchResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @Test
    void shouldRejectKnowledgeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/knowledge/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void shouldCreateKnowledgeFileMetadata() throws Exception {
        when(knowledgeService.create(any(KnowledgeFileCreateRequest.class))).thenReturn(KnowledgeFileVO.builder()
                .id(1L)
                .spaceId(1L)
                .originalName("数据库设计.md")
                .parserStatus("pending")
                .build());

        mockMvc.perform(post("/api/knowledge/files")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceId\":1,\"originalName\":\"数据库设计.md\",\"fileType\":\"md\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalName").value("数据库设计.md"));
    }

    @Test
    void shouldUploadKnowledgeFile() throws Exception {
        when(knowledgeService.upload(anyLong(), any(MultipartFile.class))).thenReturn(KnowledgeFileVO.builder()
                .id(2L)
                .spaceId(1L)
                .originalName("函数极限.md")
                .parserStatus("indexed")
                .chunkCount(1)
                .build());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "函数极限.md",
                MediaType.TEXT_MARKDOWN_VALUE,
                "函数极限是高等数学的基础。".getBytes()
        );

        mockMvc.perform(multipart("/api/knowledge/files/upload")
                        .file(file)
                        .param("spaceId", "1")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalName").value("函数极限.md"));
    }

    @Test
    void shouldAcceptKnowledgeChatHistoryAndModel() throws Exception {
        when(knowledgeService.qa(any(KnowledgeSearchRequest.class))).thenReturn(KnowledgeSearchResultVO.builder()
                .query("什么是函数极限")
                .answerMarkdown("函数极限描述趋近过程。[来源1]")
                .build());

        mockMvc.perform(post("/api/knowledge/qa")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"spaceId":1,"modelProviderId":2,"query":"什么是函数极限",\
                                "history":[{"role":"user","content":"我在复习高等数学"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerMarkdown").value("函数极限描述趋近过程。[来源1]"));
    }

    private static UsernamePasswordAuthenticationToken auth() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
