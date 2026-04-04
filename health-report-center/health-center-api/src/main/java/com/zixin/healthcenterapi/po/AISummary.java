package com.zixin.healthcenterapi.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

/**
 * AI健康总结表
 *
 * 存储AI生成的患者健康总结信息
 */
@Data
@TableName("care_platform_ai_summary")
public class AISummary implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总结ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 总结日期 (yyyy-MM-dd)
     */
    private String summaryDate;

    /**
     * 总结内容
     */
    private String content;

    /**
     * 关联的报告ID列表 (JSON数组)
     */
    private String relatedReportIds;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    private Integer dele;

    /**
     * 版本号
     */
    @Version
    private Integer version;
}