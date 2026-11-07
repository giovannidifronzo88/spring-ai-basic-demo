package com.test.spring.ai.springaibasicdemo.advisor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatMetricsAdvisorTest {

    private static final String MODEL = "test-model";

    @Test
    void recordsCallCountAndTokensFromProviderUsage() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatMetricsAdvisor advisor = new ChatMetricsAdvisor(registry);
        CallAdvisorChain chain = chainReturning(responseWithUsage(12, 34));

        advisor.adviseCall(requestWith("ciao"), chain);

        assertThat(registry.get(ChatMetricsAdvisor.CALLS_METRIC)
                .tag("gen_ai.request.model", MODEL).tag("error", "false").timer().count())
                .isEqualTo(1);
        assertThat(registry.get(ChatMetricsAdvisor.TOKENS_METRIC)
                .tag("gen_ai.request.model", MODEL).tag("gen_ai.token.type", "input").counter().count())
                .isEqualTo(12);
        assertThat(registry.get(ChatMetricsAdvisor.TOKENS_METRIC)
                .tag("gen_ai.request.model", MODEL).tag("gen_ai.token.type", "output").counter().count())
                .isEqualTo(34);
    }

    @Test
    void recordsErrorOutcomeWhenChainFails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatMetricsAdvisor advisor = new ChatMetricsAdvisor(registry);
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenThrow(new IllegalStateException("provider down"));

        try {
            advisor.adviseCall(requestWith("ciao"), chain);
        } catch (IllegalStateException expected) {
            // l'eccezione deve propagarsi; qui interessa solo la metrica
        }

        assertThat(registry.get(ChatMetricsAdvisor.CALLS_METRIC).tag("error", "true").timer().count())
                .isEqualTo(1);
    }

    @Test
    void skipsTokenCountersWhenUsageIsMissing() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatMetricsAdvisor advisor = new ChatMetricsAdvisor(registry);
        CallAdvisorChain chain = chainReturning(responseWithoutUsage());

        advisor.adviseCall(requestWith("ciao"), chain);

        assertThat(registry.find(ChatMetricsAdvisor.TOKENS_METRIC).counters()).isEmpty();
        assertThat(registry.get(ChatMetricsAdvisor.CALLS_METRIC)
                .tag("gen_ai.request.model", ChatMetricsAdvisor.UNKNOWN_MODEL).timer().count())
                .isEqualTo(1);
    }

    @Test
    void usesUnknownModelWhenMetadataModelIsNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatMetricsAdvisor advisor = new ChatMetricsAdvisor(registry);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        when(metadata.getModel()).thenReturn(null);
        CallAdvisorChain chain = chainReturning(responseWithMetadata(metadata));

        advisor.adviseCall(requestWith("ciao"), chain);

        assertThat(registry.get(ChatMetricsAdvisor.CALLS_METRIC)
                .tag("gen_ai.request.model", ChatMetricsAdvisor.UNKNOWN_MODEL).timer().count())
                .isEqualTo(1);
    }

    private CallAdvisorChain chainReturning(ChatClientResponse response) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(response);
        return chain;
    }

    private ChatClientRequest requestWith(String text) {
        return new ChatClientRequest(new Prompt(text), Map.of());
    }

    private ChatClientResponse responseWithUsage(int promptTokens, int completionTokens) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model(MODEL)
                .usage(new DefaultUsage(promptTokens, completionTokens))
                .build();
        return responseWithMetadata(metadata);
    }

    private ChatClientResponse responseWithoutUsage() {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().build();
        return responseWithMetadata(metadata);
    }

    private ChatClientResponse responseWithMetadata(ChatResponseMetadata metadata) {
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("risposta"))), metadata);
        return new ChatClientResponse(chatResponse, Map.of());
    }
}
