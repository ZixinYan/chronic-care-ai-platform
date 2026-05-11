-- ========================================
-- 医生服务 - 排班与请假表结构
-- ========================================

-- 1. 医生日程表 (doctor_schedule)
-- 存储医生的日常工作排班，复用 DoctorSchedule 实体
CREATE TABLE IF NOT EXISTS `doctor_schedule` (
    `id` BIGINT NOT NULL COMMENT '日程ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生账户ID',
    `doctor_name` VARCHAR(50) NULL COMMENT '医生姓名（冗余）',
    `patient_id` BIGINT NULL COMMENT '患者账户ID',
    `patient_name` VARCHAR(50) NULL COMMENT '患者姓名（冗余）',
    `schedule` VARCHAR(500) NOT NULL COMMENT '日程内容/描述',
    `schedule_category` VARCHAR(50) NULL COMMENT '日程类别',
    `schedule_day` VARCHAR(20) NOT NULL COMMENT '日程日期(YYYY-MM-DD)',
    `priority` INT NULL COMMENT '优先级 (1-低, 2-中, 3-高, 4-紧急)',
    `status` VARCHAR(32) NOT NULL COMMENT '日程状态(PENDING/IN_PROGRESS/COMPLETED/CANCELLED)',
    `result` TEXT NULL COMMENT '执行结果/诊断报告',
    `link` VARCHAR(255) NULL COMMENT '关联链接',
    `start_time` BIGINT NULL COMMENT '开始时间(毫秒时间戳)',
    `end_time` BIGINT NULL COMMENT '结束时间(毫秒时间戳)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON NULL COMMENT '扩展字段(JSON)',
    PRIMARY KEY (`id`),
    KEY `idx_doctor_day` (`doctor_id`, `schedule_day`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生工作日程表';

-- 2. 医生请假表 (doctor_leave)
-- 存储医生的请假记录，供排班与 AI 推荐参考
CREATE TABLE IF NOT EXISTS `doctor_leave` (
    `id` BIGINT NOT NULL COMMENT '请假单ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生账户ID',
    `doctor_name` VARCHAR(50) NULL COMMENT '医生姓名（冗余）',
    `leave_type` VARCHAR(32) NOT NULL COMMENT '请假类型(SICK/ANNUAL/PERSONAL/TRAINING/OTHER)',
    `status` VARCHAR(32) NOT NULL COMMENT '请假状态(PENDING/APPROVED/REJECTED/CANCELLED)',
    `start_day` VARCHAR(20) NOT NULL COMMENT '请假开始日期(YYYY-MM-DD)',
    `end_day` VARCHAR(20) NOT NULL COMMENT '请假结束日期(YYYY-MM-DD)',
    `start_time` BIGINT NULL COMMENT '请假开始时间(毫秒时间戳)',
    `end_time` BIGINT NULL COMMENT '请假结束时间(毫秒时间戳)',
    `reason` VARCHAR(500) NULL COMMENT '请假原因',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON NULL COMMENT '扩展字段(JSON)',
    PRIMARY KEY (`id`),
    KEY `idx_doctor` (`doctor_id`),
    KEY `idx_status` (`status`),
    KEY `idx_day_range` (`start_day`, `end_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生请假表';

