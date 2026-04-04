package com.zixin.healthcenterapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报告类型枚举
 * 
 * @author zixin
 */
@Getter
@AllArgsConstructor
public enum ReportType {
    
    /**
     * 图片报告 (PNG, JPG等)
     */
    IMAGE(1, "图片报告"),
    
    /**
     * 文字报告
     */
    TEXT(2, "文字报告"),
    
    /**
     * PDF文档
     */
    PDF(3, "PDF文档"),

    /**
     * AI生成报告
     */
    AI_GENERATED(4, "AI生成报告");

    private final Integer code;
    private final String description;
    
    /**
     * 根据code获取枚举
     */
    public static ReportType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReportType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown report type code: " + code);
    }
}
