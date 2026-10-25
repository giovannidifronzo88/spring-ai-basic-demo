package com.test.spring.ai.springaibasicdemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp")
public class McpComtroller {

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;

    public McpComtroller(ChatClient chatClient, ToolCallbackProvider mcpTools) {
        this.chatClient = chatClient;
        this.mcpTools = mcpTools;
    }

    @GetMapping("/answer")
    public String prompt(@RequestParam("prompt") String prompt, @RequestParam(value = "chatId", required = false) String chatId){

        return chatClient.prompt()
                .tools(mcpTools)
                .user(prompt)
                .call()
                .content();


    }
}
