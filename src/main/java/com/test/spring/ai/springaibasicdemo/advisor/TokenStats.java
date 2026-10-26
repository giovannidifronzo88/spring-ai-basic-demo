package com.test.spring.ai.springaibasicdemo.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public class TokenStats implements CallAdvisor {
    private static final Logger logger = LoggerFactory.getLogger(TokenStats.class);

    // Advisor rudimentale che calcola il numero di token stimati nella richiesta e logga le informazioni sui token utilizzati nella risposta.
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        int tokens = chatClientRequest.prompt()
                .getInstructions()
                .stream()
                .mapToInt(i -> i.getText()!=null? i.getText().length():0)
                .sum()/4;
        logger.info("TokenStats: estimation of tokens used in request: " + tokens);

        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        logger.info("TokenStats: Tokens according to the metadata: " + chatClientResponse.chatResponse().getMetadata().toString());

        return chatClientResponse;
    }


    @Override
    public String getName() {
        return "TokenStats";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
