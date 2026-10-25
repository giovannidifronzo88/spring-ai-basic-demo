package com.test.spring.ai.springaibasicdemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import com.test.tool.GitTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpComtroller {

    private static final Logger log = LoggerFactory.getLogger(McpComtroller.class);

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;
    private final ChatMemory chatMemory;
    private final ObjectProvider<GitTool> gitTool;


    public McpComtroller(ChatClient chatClient,
                         ToolCallbackProvider mcpTools,
                         ChatMemory chatMemory,
                         ObjectProvider<GitTool> gitTool) {
        this.chatClient = chatClient;
        this.mcpTools = mcpTools;
        this.chatMemory = chatMemory;
        this.gitTool = gitTool;
    }

    @GetMapping("/answer")
    public String prompt(@RequestParam("prompt") String prompt,
                         @RequestHeader(value = "chatId", required = true) String chatId,
                         @RequestHeader(value = "X-Git-Tool-Token", required = false) String gitToolTokenHeader) {

        var spec = chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .tools(mcpTools)
                .user(prompt);

        boolean gitToolActive = gitTool.getIfAvailable() != null;
        // Log di diagnosi: mostra se il tool è registrato e se l'header è arrivato, senza stampare il token.
        log.info("answer: chatId={}, gitToolAttivo={}, headerTokenPresente={}, prompt={}",
                chatId, gitToolActive, gitToolTokenHeader != null && !gitToolTokenHeader.isBlank(), prompt);

        // Il tool git è attivo solo con il profilo "git-tool". L'header non viene controllato qui:
        // lo passiamo al tool via toolContext, e GitTool lo verifica solo quando viene invocato.
        if (gitToolActive) {
            spec.tools(gitTool.getObject());
            spec.toolContext(Map.of("gitToolToken", gitToolTokenHeader == null ? "" : gitToolTokenHeader));
        }

        try {
            String answer = spec.call().content();
            log.info("answer: risposta chatId={}, lunghezza={}", chatId, answer == null ? 0 : answer.length());
            return answer;
        } catch (RuntimeException e) {
            log.error("answer: chiamata al modello fallita, chatId={}", chatId, e);
            throw e;
        }
    }
}

