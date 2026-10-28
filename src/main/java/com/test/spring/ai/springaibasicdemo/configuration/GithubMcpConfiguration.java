package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpRequest;

@Configuration
public class GithubMcpConfiguration {

    private static final String GITHUB_MCP_HOST = "api.githubcopilot.com";

    private final String pat;

    public GithubMcpConfiguration(@Value("${app.github.pat:}") String pat) {
        this.pat = pat;
    }

    @Bean
    McpSyncHttpClientRequestCustomizer githubAuthRequestCustomizer() {
        return this::authorizeGithubRequest;
    }

    // Il token viene inviato solo alle richieste verso GitHub, mai agli altri server MCP.
    private void authorizeGithubRequest(HttpRequest.Builder builder, String method, URI uri, String body,
                                        McpTransportContext context) {
        if (!pat.isBlank() && GITHUB_MCP_HOST.equalsIgnoreCase(uri.getHost())) {
            builder.header("Authorization", "Bearer " + pat);
        }
    }
}
