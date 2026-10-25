package com.test.spring.ai.springaibasicdemo.controller;

import com.test.spring.ai.springaibasicdemo.model.TicketAnalysis;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class SimpleController {

    public final ChatClient chatClient;
    public final ChatMemory chatMemory;

    public SimpleController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @GetMapping("/openai")
    public String testOpenAi() {
        try {
            String risposta = chatClient.prompt()
                    .user("Rispondi solo con la parola 'OK' se mi senti.")
                    .call()
                    .content();
            return "Connessione con OpenAI riuscita! Risposta del modello: " + risposta;
        } catch (Exception e) {
            return "Errore di connessione con OpenAI. Dettaglio: " + e.getMessage();
        }
    }

    @GetMapping("/answer")
    public String prompt(@RequestParam("prompt") String prompt, @RequestParam(value = "chatId", required = false) String chatId){

        return chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();


    }

    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam("prompt") String prompt) {

        return chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "my-conversation"))
                .stream()
                .content();


    }

    @GetMapping("/analysis")
    public TicketAnalysis analysis(@RequestParam("prompt") String prompt) {

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(TicketAnalysis.class);


    }
}
