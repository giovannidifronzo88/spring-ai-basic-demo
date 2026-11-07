package com.test.spring.ai.springaibasicdemo.advisor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;

/**
 * Registra per ogni chiamata al modello la latenza e i token reali riportati dal provider.
 * I dati sono esposti tramite Micrometer, allineati agli standard OpenTelemetry di Spring AI 1.0+.
 */
public class ChatMetricsAdvisor implements CallAdvisor {

    // Nomi ufficiali delle metriche secondo lo standard OpenTelemetry / Spring AI
    static final String CALLS_METRIC = "gen_ai.client.operation";
    static final String TOKENS_METRIC = "gen_ai.client.token.usage";
    static final String UNKNOWN_MODEL = "unknown";

    // Tag standardizzati
    private static final String TAG_MODEL = "gen_ai.request.model";
    private static final String TAG_OPERATION_NAME = "gen_ai.operation.name";
    private static final String TAG_TOKEN_TYPE = "gen_ai.token.type";
    private static final String TAG_ERROR = "error";

    // Valori dei tag standardizzati
    private static final String OPERATION_CHAT = "chat";
    private static final String OUTCOME_SUCCESS = "false"; // 'error=false' indica successo
    private static final String OUTCOME_ERROR = "true";   // 'error=true' indica fallimento
    private static final String TYPE_INPUT = "input";      // Sostituisce 'prompt'
    private static final String TYPE_OUTPUT = "output";    // Sostituisce 'completion'

    private final MeterRegistry meterRegistry;

    public ChatMetricsAdvisor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // Il timer parte prima della chiamata e viene fermato nel finally, così viene misurato anche in caso di errore
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatClientResponse response = null;
        String errorTag = OUTCOME_ERROR;
        try {
            response = callAdvisorChain.nextCall(chatClientRequest);
            errorTag = OUTCOME_SUCCESS;
            return response;
        } finally {
            recordCall(sample, response, errorTag);
        }
    }

    private void recordCall(Timer.Sample sample, ChatClientResponse response, String errorTag) {
        // Il modello è letto dalla risposta, quindi viene usato come tag sia per il timer sia per i token
        String model = getModel(response);
        sample.stop(Timer.builder(CALLS_METRIC)
                .tag(TAG_OPERATION_NAME, OPERATION_CHAT)
                .tag(TAG_MODEL, model)
                .tag(TAG_ERROR, errorTag)
                .register(meterRegistry));
        if (response != null) {
            Usage usage = getUsage(response);
            recordUsage(model, usage);
        }
    }

    private void recordUsage(String model, Usage usage) {
        // Non tutti i provider restituiscono l'usage: in quel caso non incrementiamo i contatori token
        if (usage == null) {
            return;
        }
        incrementTokens(model, TYPE_INPUT, usage.getPromptTokens());
        incrementTokens(model, TYPE_OUTPUT, usage.getCompletionTokens());
    }

    private void incrementTokens(String model, String tokenType, Integer tokens) {
        // Un contatore con incremento 0 comparirebbe in /actuator/metrics senza dati reali: lo saltiamo
        if (tokens == null || tokens <= 0) {
            return;
        }
        meterRegistry.counter(TOKENS_METRIC,
                TAG_MODEL, model,
                TAG_TOKEN_TYPE, tokenType
        ).increment(tokens);
    }

    private String getModel(ChatClientResponse response) {
        ChatResponseMetadata metadata = getMetadata(response);
        // Il provider può non valorizzare il modello: in quel caso usiamo un tag fisso invece di null
        if (metadata == null || metadata.getModel() == null || metadata.getModel().isBlank()) {
            return UNKNOWN_MODEL;
        }
        return metadata.getModel();
    }

    private Usage getUsage(ChatClientResponse response) {
        ChatResponseMetadata metadata = getMetadata(response);
        return metadata == null ? null : metadata.getUsage();
    }

    private ChatResponseMetadata getMetadata(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return null;
        }
        return response.chatResponse().getMetadata();
    }

    @Override
    public String getName() {
        return "ChatMetrics";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}