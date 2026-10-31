package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;

// Risponde alle richieste di sampling del server MCP usando il ChatModel dell'applicazione.
public class McpSamplingHandler {

    static final String MODEL_NAME = "spring-ai-chat-model";

    // Il ChatModel si risolve alla prima richiesta, non all'avvio: evita il ciclo con i client MCP.
    private final ObjectProvider<ChatModel> chatModel;

    public McpSamplingHandler(ObjectProvider<ChatModel> chatModel) {
        this.chatModel = chatModel;
    }

    public McpSchema.CreateMessageResult answer(McpSchema.CreateMessageRequest request) {
        Prompt prompt = toPrompt(request);
        String text = chatModel.getObject().call(prompt).getResult().getOutput().getText();
        return new McpSchema.CreateMessageResult(
                McpSchema.Role.ASSISTANT,
                new McpSchema.TextContent(text),
                MODEL_NAME,
                McpSchema.CreateMessageResult.StopReason.END_TURN);
    }

    private Prompt toPrompt(McpSchema.CreateMessageRequest request) {
        List<Message> messages = new ArrayList<>();
        addSystemPrompt(messages, request.systemPrompt());
        request.messages().forEach(samplingMessage -> messages.add(toMessage(samplingMessage)));
        return new Prompt(messages, optionsFor(request));
    }

    private void addSystemPrompt(List<Message> messages, String systemPrompt) {
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }
    }

    private Message toMessage(McpSchema.SamplingMessage samplingMessage) {
        String text = textOf(samplingMessage.content());
        return McpSchema.Role.ASSISTANT.equals(samplingMessage.role())
                ? new AssistantMessage(text)
                : new UserMessage(text);
    }

    private String textOf(McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
        }
        throw new IllegalArgumentException("Il sampling supporta solo contenuto testuale");
    }

    private ChatOptions optionsFor(McpSchema.CreateMessageRequest request) {
        ChatOptions.Builder<?> builder = ChatOptions.builder();
        if (request.maxTokens() != null) {
            builder.maxTokens(request.maxTokens());
        }
        return builder.build();
    }
}
