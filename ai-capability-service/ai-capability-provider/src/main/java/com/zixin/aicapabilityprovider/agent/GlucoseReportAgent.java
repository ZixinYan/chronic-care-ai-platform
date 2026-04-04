package com.zixin.aicapabilityprovider.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * 血糖健康报告AI Agent配置
 *
 * 使用糖尿病健康管理知识库（RAG）+血糖数据，
 * 生成个性化的健康报告和建议。
 */
@Configuration
public class GlucoseReportAgent {

    @Value("classpath:skills/glucose-health-report-prompt.md")
    private Resource systemPromptResource;

    @Bean("glucoseReportChatClient")
    ChatClient glucoseReportChatClient(ChatClient.Builder builder) throws Exception {
        String systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(),
                StandardCharsets.UTF_8
        );
        return builder.defaultSystem(systemPrompt).build();
    }
}
