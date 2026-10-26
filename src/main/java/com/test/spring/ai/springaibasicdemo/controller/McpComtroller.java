package com.test.spring.ai.springaibasicdemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.test.tool.GitTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;

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
    public ResponseEntity<String> prompt(@RequestParam("prompt") String prompt,
                         @RequestHeader(value = "chatId", required = true) String chatId,
                         @RequestHeader(value = "X-Git-Tool-Token", required = false) String gitToolTokenHeader,
                         @RequestHeader(value = "X-Git-Approval", required = false) String gitApprovalHeader) {

        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .tools(mcpTools)
                .user(prompt);

        boolean gitToolActive = gitTool.getIfAvailable() != null;
        // Log di diagnosi: mostra se il tool è registrato e se l'header è arrivato, senza stampare il token.
        log.info("answer: chatId={}, gitToolAttivo={}, headerTokenPresente={}, prompt={}",
                chatId, gitToolActive, gitToolTokenHeader != null && !gitToolTokenHeader.isBlank(), prompt);

        // Il tool git è attivo solo con il profilo "git-tool". Gli header HTTP non vengono verificati qui:
        // vengono copiati nel ToolContext, che il framework passa al tool quando il modello lo invoca.
        if (gitToolActive) {
            spec.tools(gitTool.getObject());
            // chatId lega l'approvazione alla chat; X-Git-Approval è l'ID che la UI rimanda solo al click di conferma.
            spec.toolContext(Map.of(
                    "gitToolToken", gitToolTokenHeader == null ? "" : gitToolTokenHeader,
                    "chatId", chatId,
                    "gitApproval", gitApprovalHeader == null ? "" : gitApprovalHeader));
        }

        try {
            String answer = spec.call().content();
            log.info("answer: risposta chatId={}, lunghezza={}", chatId, answer == null ? 0 : answer.length());
            ResponseEntity.BodyBuilder response = ResponseEntity.ok();
            if (gitToolActive) {
                // L'ID dell'approvazione pendente va alla UI tramite header, non nel testo che legge il modello.
                Optional<String> approvalId = gitTool.getObject().approvalId(chatId);
                approvalId.ifPresent(id -> response.header("X-Git-Approval-Id", id));
            }
            return response.body(answer);
        } catch (RuntimeException e) {
            log.error("answer: chiamata al modello fallita, chatId={}", chatId, e);
            throw e;
        }
    }
}

