-- 电子病历表
-- 用于存储 AI 生成的电子病历记录

CREATE TABLE `medical_record` (
    `id` BIGINT NOT NULL COMMENT '病历ID (主键)',
    `schedule_id` BIGINT NOT NULL COMMENT '关联日程ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名（冗余字段）',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `patient_name` VARCHAR(64) DEFAULT NULL COMMENT '患者姓名（冗余字段）',
    `visit_date` VARCHAR(10) NOT NULL COMMENT '就诊日期 (格式: YYYY-MM-DD)',
    `visit_type` VARCHAR(64) DEFAULT NULL COMMENT '就诊类型',
    `chief_complaint` TEXT COMMENT '主诉',
    `present_illness` TEXT COMMENT '现病史',
    `past_history` TEXT COMMENT '既往史',
    `diagnosis` TEXT COMMENT '诊断意见',
    `treatment_plan` TEXT COMMENT '治疗建议',
    `prescription` TEXT COMMENT '处方信息',
    `precautions` TEXT COMMENT '注意事项',
    `follow_up_advice` TEXT COMMENT '随访建议',
    `full_content` MEDIUMTEXT COMMENT '完整病历内容 (Markdown格式)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间',
    `update_time` BIGINT NOT NULL COMMENT '更新时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号 (乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段 (JSON格式)',
    PRIMARY KEY (`id`),
    KEY `idx_schedule_id` (`schedule_id`) COMMENT '日程ID索引',
    KEY `idx_doctor_id` (`doctor_id`) COMMENT '医生ID索引',
    KEY `idx_patient_id` (`patient_id`) COMMENT '患者ID索引',
    KEY `idx_visit_date` (`visit_date`) COMMENT '就诊日期索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子病历表';

-- 医生请假表（如果尚未创建）
CREATE TABLE IF NOT EXISTS `doctor_leave` (
    `id` BIGINT NOT NULL COMMENT '请假单ID (主键)',
    `doctor_id` BIGINT NOT NULL COMMENT '医生账户ID',
    `doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名（冗余字段，方便查询展示）',
    `leave_type` VARCHAR(32) NOT NULL COMMENT '请假类型（SICK/ANNUAL/PERSONAL/TRAINING/OTHER）',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '请假状态（PENDING/APPROVED/REJECTED/CANCELLED）',
    `start_day` VARCHAR(10) NOT NULL COMMENT '请假开始日期 (格式: YYYY-MM-DD)',
    `end_day` VARCHAR(10) NOT NULL COMMENT '请假结束日期 (格式: YYYY-MM-DD)',
    `start_time` BIGINT DEFAULT NULL COMMENT '请假开始时间（毫秒时间戳，可选）',
    `end_time` BIGINT DEFAULT NULL COMMENT '请假结束时间（毫秒时间戳，可选）',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '请假原因',
    `create_time` BIGINT NOT NULL COMMENT '创建时间',
    `update_time` BIGINT NOT NULL COMMENT '更新时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号 (乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段 (JSON格式)',
    PRIMARY KEY (`id`),
    KEY `idx_doctor_id` (`doctor_id`) COMMENT '医生ID索引',
    KEY `idx_status` (`status`) COMMENT '状态索引',
    KEY `idx_start_day` (`start_day`) COMMENT '开始日期索引',
    KEY `idx_date_range` (`start_day`, `end_day`) COMMENT '日期范围索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生请假表';
