package com.zixin.healthcenterapi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报告分类枚举
 * 
 * @author zixin
 */
@Getter
@AllArgsConstructor
public enum ReportCategory {
    
    BLOOD_SUGAR("blood_sugar", "血糖检测"),
    BLOOD_PRESSURE("blood_pressure", "血压检测"),
    ECG("ecg", "心电图"),
    BLOOD_ROUTINE("blood_routine", "血常规"),
    URINE_ROUTINE("urine_routine", "尿常规"),
    GLUCOSE_PREDICTION("GLUCOSE_PREDICTION", "血糖预测报告"),
    PHYSICAL_EXAMINATION("physical_examination", "体检报告"),
    IMAGING("imaging", "影像检查"),
    PATHOLOGY("pathology", "病理报告"),
    OTHER("other", "其他");

    private final String code;
    private final String description;
    
    public static ReportCategory fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (ReportCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return OTHER;
    }
}
