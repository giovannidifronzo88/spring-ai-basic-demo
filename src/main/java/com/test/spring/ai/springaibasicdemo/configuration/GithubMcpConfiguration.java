package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubMcpConfiguration {

    private static final String GITHUB_MCP_HOST = "api.githubcopilot.com";

    @Bean
    McpSyncHttpClientRequestCustomizer githubAuthRequestCustomizer(@Value("${app.github.pat:}") String pat) {
        return (builder, method, uri, body, context) -> {
            // Il token viene inviato solo alle richieste verso GitHub, mai agli altri server MCP.
            if (!pat.isBlank() && GITHUB_MCP_HOST.equalsIgnoreCase(uri.getHost())) {
                builder.header("Authorization", "Bearer " + pat);
            }
        };
    }
}
