package com.test.spring.ai.springaibasicdemo.advisor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

/**
 * Maschera i dati personali presenti nei messaggi utente prima che la richiesta raggiunga il modello.
 * Vengono loggate solo le categorie rilevate, mai i valori originali.
 */
public class SensitiveDataMaskerAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataMaskerAdvisor.class);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // La richiesta originale non viene modificata: ne creiamo una copia con il prompt già mascherato.
        ChatClientRequest maskedRequest = chatClientRequest.mutate()
                .prompt(maskPrompt(chatClientRequest.prompt()))
                .build();
        return callAdvisorChain.nextCall(maskedRequest);
    }

    // Ricostruisce il prompt sostituendo solo i messaggi utente; system e assistant restano invariati.
    private Prompt maskPrompt(Prompt prompt) {
        List<Message> maskedMessages = prompt.getInstructions().stream()
                .map(this::maskMessage)
                .collect(Collectors.toList());
        return new Prompt(maskedMessages, prompt.getOptions());
    }

    private Message maskMessage(Message message) {
        if (message instanceof UserMessage userMessage) {
            return maskUserMessage(userMessage);
        }
        return message;
    }

    private Message maskUserMessage(UserMessage userMessage) {
        String originalText = userMessage.getText();
        if (originalText == null) {
            return userMessage;
        }
        List<PiiCategory> detected = detectedCategories(originalText);
        if (detected.isEmpty()) {
            return userMessage;
        }
        log.info("dati personali mascherati, categorie: {}", detected);
        // mutate() conserva metadati e tipo del messaggio, sostituendo solo il testo.
        return userMessage.mutate()
                .text(maskText(originalText))
                .build();
    }

    private List<PiiCategory> detectedCategories(String text) {
        return Arrays.stream(PiiCategory.values())
                .filter(category -> category.appearsIn(text))
                .toList();
    }

    // Le categorie vengono applicate in sequenza: ogni passaggio lavora sul risultato del precedente.
    private String maskText(String text) {
        String masked = text;
        for (PiiCategory category : PiiCategory.values()) {
            masked = category.maskIn(masked);
        }
        return masked;
    }

    @Override
    public String getName() {
        return "PiiGuardrail";
    }

    // HIGHEST_PRECEDENCE: eseguito per primo, così logger e metriche ricevono già il prompt mascherato.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
