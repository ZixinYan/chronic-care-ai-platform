-- =============================================
-- AI健康总结表
-- 存储AI生成的患者健康总结信息
-- =============================================

CREATE TABLE IF NOT EXISTS `care_platform_ai_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '总结ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `summary_date` VARCHAR(10) NOT NULL COMMENT '总结日期 (yyyy-MM-dd)',
    `content` TEXT COMMENT '总结内容',
    `related_report_ids` VARCHAR(500) COMMENT '关联的报告ID列表 (JSON数组)',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `dele` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号 (乐观锁)',
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_summary_date` (`summary_date`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI健康总结表';

-- =============================================
-- 血糖数据表 (用于存储原始血糖监测数据)
-- =============================================

CREATE TABLE IF NOT EXISTS `care_platform_glucose_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `data_type` TINYINT NOT NULL DEFAULT 1 COMMENT '数据类型：1-CGM, 2-指尖血',
    `glucose_value` DECIMAL(5,2) NOT NULL COMMENT '血糖值 (mg/dL)',
    `record_time` BIGINT NOT NULL COMMENT '记录时间戳',
    `meal_type` TINYINT DEFAULT 1 COMMENT '用餐类型：1-空腹, 2-餐后1h, 3-餐后2h, 4-餐后3h',
    `device_id` VARCHAR(50) COMMENT '设备ID',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    `update_time` BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间',
    `dele` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_record_time` (`record_time`),
    KEY `idx_patient_record_time` (`patient_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='血糖数据表';

-- =============================================
-- 血糖预测记录表
-- =============================================

CREATE TABLE IF NOT EXISTS `care_platform_glucose_prediction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预测ID',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `predict_start_time` BIGINT NOT NULL COMMENT '预测起始时间',
    `predict_hours` INT NOT NULL DEFAULT 3 COMMENT '预测时长（小时）',
    `predicted_values` TEXT COMMENT '预测血糖值列表 (JSON数组)',
    `confidence` DECIMAL(3,2) DEFAULT 0.85 COMMENT '预测置信度',
    `meal_type` TINYINT DEFAULT 1 COMMENT '用餐类型',
    `alert_triggered` TINYINT DEFAULT 0 COMMENT '是否触发预警：0-否, 1-是',
    `create_time` BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_predict_time` (`predict_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='血糖预测记录表';