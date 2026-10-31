package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpSamplingHandlerTest {

    @Test
    void answersSamplingRequestWithChatModelText() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage("Itinerario pronto"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        McpSamplingHandler handler = new McpSamplingHandler(providerOf(chatModel));

        McpSchema.CreateMessageRequest request = McpSchema.CreateMessageRequest.builder()
                .messages(List.of(new McpSchema.SamplingMessage(
                        McpSchema.Role.USER, new McpSchema.TextContent("Crea un itinerario"))))
                .systemPrompt("Sei un assistente di viaggio")
                .maxTokens(500)
                .build();

        McpSchema.CreateMessageResult result = handler.answer(request);

        assertThat(result.role()).isEqualTo(McpSchema.Role.ASSISTANT);
        assertThat(((McpSchema.TextContent) result.content()).text()).isEqualTo("Itinerario pronto");
        assertThat(result.stopReason()).isEqualTo(McpSchema.CreateMessageResult.StopReason.END_TURN);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> providerOf(ChatModel chatModel) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(chatModel);
        return provider;
    }
}
