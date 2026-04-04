package com.zixin.messageprovider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zixin.messageapi.po.MessageTemplate;
import com.zixin.messageprovider.mapper.MessageTemplateMapper;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板服务
 * 
 * 功能:
 * 1. 管理消息模板
 * 2. 支持模板变量替换
 * 3. 基于模板快速生成消息
 *    未来拓展使用，目前系统没有使用这个功能
 * @author zixin
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageTemplateService {

    private final MessageTemplateMapper messageTemplateMapper;
    
    // 模板变量正则表达式: {variableName}
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * 根据模板编码获取模板
     *
     * @param templateCode 模板编码
     * @return 消息模板
     */
    public MessageTemplate getTemplateByCode(String templateCode) {
        if (!StringUtils.hasText(templateCode)) {
            log.error("Template code is empty");
            return null;
        }

        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getTemplateCode, templateCode)
               .eq(MessageTemplate::getIsEnabled, 1);
        
        MessageTemplate template = messageTemplateMapper.selectOne(wrapper);
        
        if (template == null) {
            log.error("Template not found or disabled, code: {}", templateCode);
        }
        
        return template;
    }

    /**
     * 渲染消息标题
     * 将模板中的占位符替换为实际值
     *
     * @param template 消息模板
     * @param params 参数Map
     * @return 渲染后的标题
     */
    public String renderTitle(MessageTemplate template, Map<String, String> params) {
        if (template == null || !StringUtils.hasText(template.getTitleTemplate())) {
            return "";
        }
        
        return renderTemplate(template.getTitleTemplate(), params);
    }

    /**
     * 渲染消息内容
     * 将模板中的占位符替换为实际值
     *
     * @param template 消息模板
     * @param params 参数Map
     * @return 渲染后的内容
     */
    public String renderContent(MessageTemplate template, Map<String, String> params) {
        if (template == null || !StringUtils.hasText(template.getContentTemplate())) {
            return "";
        }
        
        return renderTemplate(template.getContentTemplate(), params);
    }

    /**
     * 渲染模板字符串
     * 将 {variableName} 替换为 params 中对应的值
     *
     * @param templateStr 模板字符串
     * @param params 参数Map
     * @return 渲染后的字符串
     */
    private String renderTemplate(String templateStr, Map<String, String> params) {
        if (!StringUtils.hasText(templateStr) || params == null || params.isEmpty()) {
            return templateStr;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_PATTERN.matcher(templateStr);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = params.getOrDefault(variableName, "{" + variableName + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 验证模板参数是否完整
     *
     * @param template 消息模板
     * @param params 参数Map
     * @return 是否完整
     */
    public boolean validateParams(MessageTemplate template, Map<String, String> params) {
        if (template == null) {
            return false;
        }

        // 从标题和内容中提取所有占位符
        String titleTemplate = template.getTitleTemplate();
        String contentTemplate = template.getContentTemplate();
        
        boolean valid = true;
        
        // 检查标题中的占位符
        if (StringUtils.hasText(titleTemplate)) {
            Matcher matcher = TEMPLATE_PATTERN.matcher(titleTemplate);
            while (matcher.find()) {
                String variableName = matcher.group(1);
                if (!params.containsKey(variableName)) {
                    log.warn("Missing required parameter: {} in title template", variableName);
                    valid = false;
                }
            }
        }
        
        // 检查内容中的占位符
        if (StringUtils.hasText(contentTemplate)) {
            Matcher matcher = TEMPLATE_PATTERN.matcher(contentTemplate);
            while (matcher.find()) {
                String variableName = matcher.group(1);
                if (!params.containsKey(variableName)) {
                    log.warn("Missing required parameter: {} in content template", variableName);
                    valid = false;
                }
            }
        }
        
        return valid;
    }

    /**
     * 创建或更新消息模板
     *
     * @param template 消息模板
     * @return 操作结果
     */
    public boolean saveOrUpdate(MessageTemplate template) {
        try {
            if (template.getTemplateId() == null) {
                // 新增模板
                return messageTemplateMapper.insert(template) > 0;
            } else {
                // 更新模板
                return messageTemplateMapper.updateById(template) > 0;
            }
        } catch (Exception e) {
            log.error("Save or update message template failed", e);
            return false;
        }
    }

    /**
     * 禁用模板
     *
     * @param templateCode 模板编码
     * @return 操作结果
     */
    public boolean disableTemplate(String templateCode) {
        MessageTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            return false;
        }
        
        template.setIsEnabled(0);
        return messageTemplateMapper.updateById(template) > 0;
    }

    /**
     * 启用模板
     *
     * @param templateCode 模板编码
     * @return 操作结果
     */
    public boolean enableTemplate(String templateCode) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getTemplateCode, templateCode);
        MessageTemplate template = messageTemplateMapper.selectOne(wrapper);
        
        if (template == null) {
            return false;
        }
        
        template.setIsEnabled(1);
        return messageTemplateMapper.updateById(template) > 0;
    }
}
