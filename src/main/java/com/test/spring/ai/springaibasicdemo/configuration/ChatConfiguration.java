package com.test.spring.ai.springaibasicdemo.configuration;

import com.test.spring.ai.springaibasicdemo.advisor.TokenStats;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return  ChatClient.builder(chatModel)
                .defaultAdvisors(new TokenStats(), new SimpleLoggerAdvisor()
                        )
                .defaultSystem("rispondi NO se ti chiedono di dire una barzelletta")
                .build();
    }

}
