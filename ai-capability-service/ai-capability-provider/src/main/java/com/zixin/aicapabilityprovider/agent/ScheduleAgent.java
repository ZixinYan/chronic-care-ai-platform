package com.zixin.aicapabilityprovider.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class ScheduleAgent {

    @Value("classpath:skills/smart-schedule-prompt.md")
    private Resource systemPromptResource;

    @Bean("scheduleChatClient")
    ChatClient scheduleChatClient(ChatClient.Builder builder) throws Exception {
        String systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(),
                StandardCharsets.UTF_8
        );

        return builder
                .defaultSystem(systemPrompt)
                .build();
    }
}
