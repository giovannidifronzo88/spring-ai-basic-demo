package com.test.spring.ai.springaibasicdemo.configuration;

import com.test.tool.GitTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("git-tool")
public class GitToolConfiguration {

    // Espone solo gitPushIfNeeded all'LLM, ed è attivo solo con il profilo "git-tool".
    // Il token atteso viene da app.git.tool.token (application.properties, a sua volta da APP_GIT_TOOL_TOKEN) e viene verificato dentro il tool, non nel controller.
    @Bean
    public GitTool gitTool(@Value("${app.git.tool.token:}") String expectedToken) {
        return new GitTool(expectedToken);
    }
}
