package com.zixin.aicapabilityprovider.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * AI 电子病历生成：ChatClient 负责根据诊疗信息生成结构化电子病历。
 *
 * 采用 Code-first ReAct 模式：
 * - Act（工具执行）由 MedicalRecordTools 完成
 * - Reason（推理生成）由本 Client 完成
 */
@Configuration
public class MedicalRecordAgent {

    @Value("classpath:skills/medical-record-prompt.md")
    private Resource systemPromptResource;

    /**
     * 电子病历生成专用客户端。
     */
    @Bean("medicalRecordChatClient")
    ChatClient medicalRecordChatClient(ChatClient.Builder builder) throws Exception {
        String systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(),
                StandardCharsets.UTF_8
        );
        return builder.defaultSystem(systemPrompt).build();
    }
}
