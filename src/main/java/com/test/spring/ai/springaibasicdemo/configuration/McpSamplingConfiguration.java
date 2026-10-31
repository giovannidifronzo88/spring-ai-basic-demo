package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpSamplingConfiguration {


    @Bean
    McpClientCustomizer<McpClient.SyncSpec> mcpSamplingCustomizer(ObjectProvider<ChatModel> chatModel) {
        McpSamplingHandler handler = new McpSamplingHandler(chatModel);
        return (connectionName, spec) -> {
            spec.capabilities(McpSchema.ClientCapabilities.builder().sampling().build());
            spec.sampling(handler::answer);
        };
    }
}
