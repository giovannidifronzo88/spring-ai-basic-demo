package com.test.spring.ai.springaibasicdemo.configuration;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.function.Supplier;

// Il provider viene interrogato a ogni richiesta, cosi' il JWT del server locale e' sempre fresco.
public record McpServerCredential(String host, Supplier<String> bearerTokenProvider) {

    public boolean isFor(URI uri) {
        return host.equalsIgnoreCase(uri.getHost());
    }

    public void authorize(HttpRequest.Builder builder) {
        String bearerToken = bearerTokenProvider.get();
        if (!bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
    }
}
