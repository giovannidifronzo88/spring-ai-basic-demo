package com.test.spring.ai.springaibasicdemo.advisor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensitiveDataMaskerAdvisorTest {

    private static final String SYSTEM_TEXT = "rispondi in italiano";

    @Test
    void masksPersonalDataInUserMessageBeforeCallingModel() {
        String userText = "sono mario.rossi@example.it, CF RSSMRA80A01H501U";
        ChatClientRequest forwarded = adviseAndCapture(userText);

        assertThat(userMessages(forwarded)).containsExactly("sono [EMAIL], CF [CODICE_FISCALE]");
    }

    @Test
    void leavesSystemMessageUntouched() {
        ChatClientRequest forwarded = adviseAndCapture("ciao");

        assertThat(forwarded.prompt().getSystemMessage().getText()).isEqualTo(SYSTEM_TEXT);
    }

    @Test
    void forwardsPlainUserMessageUnchanged() {
        ChatClientRequest forwarded = adviseAndCapture("quante ore di ferie spettano?");

        assertThat(userMessages(forwarded)).containsExactly("quante ore di ferie spettano?");
    }

    @Test
    void returnsResponseFromChain() {
        SensitiveDataMaskerAdvisor advisor = new SensitiveDataMaskerAdvisor();
        ChatClientResponse expected = responseWithText("risposta");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(expected);

        ChatClientResponse actual = advisor.adviseCall(requestWithUser("ciao"), chain);

        assertThat(actual).isSameAs(expected);
    }

    private ChatClientRequest adviseAndCapture(String userText) {
        SensitiveDataMaskerAdvisor advisor = new SensitiveDataMaskerAdvisor();
        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        when(chain.nextCall(any())).thenReturn(responseWithText("ok"));

        advisor.adviseCall(requestWithUser(userText), chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
        verify(chain).nextCall(captor.capture());
        return captor.getValue();
    }

    private ChatClientRequest requestWithUser(String userText) {
        List<Message> messages = List.of(new SystemMessage(SYSTEM_TEXT), new UserMessage(userText));
        return new ChatClientRequest(new Prompt(messages), Map.of());
    }

    private List<String> userMessages(ChatClientRequest request) {
        return request.prompt().getUserMessages().stream()
                .map(UserMessage::getText)
                .toList();
    }

    private ChatClientResponse responseWithText(String text) {
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        return new ChatClientResponse(chatResponse, Map.of());
    }
}
