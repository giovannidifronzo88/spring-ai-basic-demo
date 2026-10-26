package com.test.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Controlla il token passato dalla richiesta HTTP. Il token arriva via ToolContext, non dal modello.
class GitAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(GitAccessGuard.class);

    private final String expectedToken;

    GitAccessGuard(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    // Fail-closed: se APP_GIT_TOOL_TOKEN è vuoto, nessuna invocazione è autorizzata.
    boolean isAuthorized(ToolContext toolContext) {
        String provided = String.valueOf(toolContext.getContext().getOrDefault("gitToolToken", ""));
        boolean ok = !expectedToken.isBlank()
                && MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                                         provided.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            log.warn("git-tool: invocazione rifiutata, token mancante o errato");
        }
        return ok;
    }
}
