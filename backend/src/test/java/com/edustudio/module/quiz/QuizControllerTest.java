package com.edustudio.module.quiz;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.profile.vo.MasteryRecordVO;
import com.edustudio.module.quiz.dto.QuizGenerateRequest;
import com.edustudio.module.quiz.dto.QuizSubmitRequest;
import com.edustudio.module.quiz.service.QuizService;
import com.edustudio.module.quiz.vo.QuizResultVO;
import com.edustudio.module.quiz.vo.QuizVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizService quizService;

    @Test
    void shouldRejectQuizWithoutToken() throws Exception {
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateSubmitAndReturnMastery() throws Exception {
        when(quizService.generate(any(QuizGenerateRequest.class))).thenReturn(QuizVO.builder().id(1L).title("数据库测验").build());
        when(quizService.submit(eq(1L), any(QuizSubmitRequest.class))).thenReturn(QuizResultVO.builder().quizId(1L).score(BigDecimal.TEN).totalScore(BigDecimal.TEN).build());
        when(quizService.mastery()).thenReturn(List.of(MasteryRecordVO.builder().knowledgePoint("索引").masteryLevel(BigDecimal.valueOf(90)).build()));

        mockMvc.perform(post("/api/quizzes/generate")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceId\":1,\"subject\":\"数据库\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("数据库测验"));

        mockMvc.perform(post("/api/quizzes/1/submit")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":1,\"answerText\":\"B\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(10));

        mockMvc.perform(get("/api/mastery/me").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].knowledgePoint").value("索引"));
    }

    private static UsernamePasswordAuthenticationToken auth() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
