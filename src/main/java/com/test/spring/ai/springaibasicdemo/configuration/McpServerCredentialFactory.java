package com.test.spring.ai.springaibasicdemo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

@Component
public class McpServerCredentialFactory {

    private static final String GITHUB_MCP_HOST = "api.githubcopilot.com";

    private final String githubPat;
    private final String localServerHost;
    private final String localServerPrivateKeyPath;

    public McpServerCredentialFactory(
            @Value("${app.github.pat:}") String githubPat,
            @Value("${app.mcp.local-server.host:127.0.0.1}") String localServerHost,
            @Value("${app.mcp.local-server.private-key-path:}") String localServerPrivateKeyPath) {
        this.githubPat = githubPat;
        this.localServerHost = localServerHost;
        this.localServerPrivateKeyPath = localServerPrivateKeyPath;
    }

    public List<McpServerCredential> createCredentials() {
        return List.of(
                new McpServerCredential(GITHUB_MCP_HOST, () -> githubPat),
                new McpServerCredential(localServerHost, localServerBearerTokenProvider()));
    }

    // Senza chiave privata non viene inviato alcun Authorization: il server rifiuta la richiesta (fail-closed).
    private Supplier<String> localServerBearerTokenProvider() {
        if (localServerPrivateKeyPath.isBlank()) {
            return () -> "";
        }
        McpClientJwtSigner signer = McpClientJwtSigner.fromPemFile(Path.of(localServerPrivateKeyPath));
        return signer::issueToken;
    }
}
