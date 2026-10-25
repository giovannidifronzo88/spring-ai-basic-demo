package com.test.spring.ai.springaibasicdemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        // Nessun server MCP esterno: il client non deve connettersi all'avvio
        "spring.ai.mcp.client.enabled=false",
        // Nessuna dipendenza dalla variabile d'ambiente OPEN_AI_KEY
        "spring.ai.openai.api-key=test-key",
        // Nessuna creazione di collection su Qdrant Cloud
        "spring.ai.vectorstore.qdrant.initialize-schema=false",
        "app.qdrant.collections.ccnl.initialize-schema=false"
})
class SpringAiBasicDemoApplicationTests {

	@MockitoBean
	ChatModel chatModel;

	@MockitoBean
	EmbeddingModel embeddingModel;

	@MockitoBean(name = "baseVectorStore")
	VectorStore baseVectorStore;

	@MockitoBean(name = "ccnlVectorStore")
	VectorStore ccnlVectorStore;

	@TestConfiguration
	static class NoMcpToolsConfiguration {

		// Sostituisce il provider MCP: nessun tool remoto, nessun client MCP
		@Bean
		ToolCallbackProvider mcpTools() {
			return () -> new ToolCallback[0];
		}
	}

	@Test
	void contextLoads() {
	}

}
