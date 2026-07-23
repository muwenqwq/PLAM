package com.edustudio.integration.ai;

import com.edustudio.integration.ai.dto.AiChatRequest;
import com.edustudio.integration.ai.dto.AiChatResponse;
import com.edustudio.integration.ai.dto.AiModelConfigDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceClientTest {

    @Test
    void shouldConsumeAiServiceStreamIncrementallyAndReturnFinalResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiServiceClient client = new AiServiceClient(builder, new ObjectMapper());
        ReflectionTestUtils.setField(client, "baseUrl", "http://ai.test");

        server.expect(once(), requestTo("http://ai.test/ai/chat/stream"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        data: {"type":"delta","content":"先理解 "}

                        data: {"type":"delta","content":"B+Tree"}

                        data: {"type":"done","response":{"provider_type":"mock","model_name":"mock-chat-v1","reply_markdown":"先理解 B+Tree","reply_json":{"subject":"数据库"},"token_count":8}}

                        """, MediaType.TEXT_EVENT_STREAM));

        List<String> deltas = new ArrayList<>();
        AiChatResponse response = client.streamChat(AiChatRequest.builder()
                .modelConfig(AiModelConfigDTO.builder()
                        .providerType("mock")
                        .modelName("mock-chat-v1")
                        .build())
                .message("解释索引")
                .build(), deltas::add);

        assertThat(deltas).containsExactly("先理解 ", "B+Tree");
        assertThat(response.getReplyMarkdown()).isEqualTo("先理解 B+Tree");
        assertThat(response.getTokenCount()).isEqualTo(8);
        server.verify();
    }
}
