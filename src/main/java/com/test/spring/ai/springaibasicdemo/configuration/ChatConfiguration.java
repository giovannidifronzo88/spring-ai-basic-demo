package com.test.spring.ai.springaibasicdemo.configuration;

import com.test.spring.ai.springaibasicdemo.advisor.ChatMetricsAdvisor;
import com.test.spring.ai.springaibasicdemo.advisor.SensitiveDataMaskerAdvisor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfiguration {

    @Bean
    ChatClient chatClient(ChatModel chatModel, MeterRegistry meterRegistry) {
        return  ChatClient.builder(chatModel)
                .defaultAdvisors(new SensitiveDataMaskerAdvisor(), new ChatMetricsAdvisor(meterRegistry), new SimpleLoggerAdvisor()
                        )
                .defaultSystem("rispondi in italiano, sii conciso e chiaro, non scrivere nulla che non sia strettamente necessario")
                .build();
    }

}
