package com.test.spring.ai.springaibasicdemo.configuration;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

@Configuration
public class McpAuthorizationConfiguration {

    private final List<McpServerCredential> serverCredentials;

    public McpAuthorizationConfiguration(McpServerCredentialFactory credentialFactory) {
        this.serverCredentials = credentialFactory.createCredentials();
    }

    @Bean
    McpSyncHttpClientRequestCustomizer mcpAuthorizationRequestCustomizer() {
        return this::authorizeRequest;
    }

    // Ogni token viene inviato solo all'host a cui appartiene, mai agli altri server MCP.
    private void authorizeRequest(HttpRequest.Builder builder, String method, URI uri, String body,
                                  McpTransportContext context) {
        serverCredentials.stream()
                .filter(credential -> credential.isFor(uri))
                .findFirst()
                .ifPresent(credential -> credential.authorize(builder));
    }
}
