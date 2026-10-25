package com.test.spring.ai.springaibasicdemo.controller;

import com.test.tool.DateTimeTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/tool")
public class ToolController {

    private final ChatClient chatClient;

    public ToolController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/time")
    public String tool(@RequestParam("prompt") String prompt, @RequestHeader(value = "chatId", required = false) String chatId) {

        return chatClient.prompt()
                .user(prompt)
                .system("Rispondi esclusivamente alle domande riguardanti l'orario e la data attuale. Se la domanda non riguarda l'orario o la data, rispondi che non lo sai")
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .tools(new DateTimeTool())
                .call()
                .content();
    }
}
