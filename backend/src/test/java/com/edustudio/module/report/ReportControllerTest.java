package com.edustudio.module.report;

import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.report.dto.ReportGenerateRequest;
import com.edustudio.module.report.service.ReportService;
import com.edustudio.module.report.vo.LearningReportVO;
import com.edustudio.module.report.vo.ReportOverviewVO;
import org.hamcrest.Matchers;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void shouldRejectReportWithoutToken() throws Exception {
        mockMvc.perform(get("/api/reports/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnOverviewAndGenerateReport() throws Exception {
        when(reportService.overview(null)).thenReturn(ReportOverviewVO.builder().resourceCount(2L).quizCount(1L).averageMastery(BigDecimal.valueOf(80)).build());
        when(reportService.generate(any(ReportGenerateRequest.class))).thenReturn(LearningReportVO.builder().id(1L).title("学习周报").summary("summary").build());

        mockMvc.perform(get("/api/reports/overview").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceCount").value(2));

        mockMvc.perform(post("/api/reports/generate")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"spaceId\":1,\"title\":\"学习周报\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("学习周报"));
    }

    @Test
    void shouldDownloadReportMarkdown() throws Exception {
        when(reportService.detail(1L)).thenReturn(LearningReportVO.builder().id(1L).title("学习周报").build());
        when(reportService.exportMarkdown(1L)).thenReturn("# 学习周报\n\n## 建议\n继续复习索引");

        mockMvc.perform(get("/api/reports/1/download?format=md").with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("filename*=UTF-8''")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".md")))
                .andExpect(content().string("# 学习周报\n\n## 建议\n继续复习索引"));
    }

    @Test
    void shouldDeleteOwnedReport() throws Exception {
        mockMvc.perform(delete("/api/reports/1").with(authentication(auth())))
                .andExpect(status().isOk());
    }

    private static UsernamePasswordAuthenticationToken auth() {
        UserPrincipal principal = new UserPrincipal(3L, "demo_student", "", "演示学生", "active", List.of("STUDENT"));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
