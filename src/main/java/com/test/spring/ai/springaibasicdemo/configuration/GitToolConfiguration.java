package com.test.spring.ai.springaibasicdemo.configuration;

import com.test.tool.GitTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("git-tool")
public class GitToolConfiguration {

    // Espone al modello i tool git (push, creazione branch, merge), attivi solo con il profilo "git-tool".
    // Il token atteso viene da app.git.tool.token (da APP_GIT_TOOL_TOKEN) e viene verificato da GitAccessGuard al momento della chiamata.
    @Bean
    public GitTool gitTool(@Value("${app.git.tool.token:}") String expectedToken,
                           @Value("${app.github.pat:}") String githubPat) {
        return new GitTool(expectedToken, githubPat);
    }
}
