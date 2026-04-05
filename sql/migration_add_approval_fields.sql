-- ============================================================
-- 数据库迁移脚本: 为 doctor_leave 表添加审批相关字段
-- 日期: 2026-04-05
-- 问题: 实体类 DoctorLeave 中定义了审批字段，但数据库表缺少这些列
-- ============================================================

-- 添加审批人ID字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID' AFTER `reason`;

-- 添加审批人姓名字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approver_name` VARCHAR(64) DEFAULT NULL COMMENT '审批人姓名' AFTER `approver_id`;

-- 添加审批意见字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approval_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见' AFTER `approver_name`;

-- 添加审批时间字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approval_time` BIGINT DEFAULT NULL COMMENT '审批时间(毫秒时间戳)' AFTER `approval_comment`;

-- 验证字段是否添加成功
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'doctor_leave' 
-- AND COLUMN_NAME IN ('approver_id', 'approver_name', 'approval_comment', 'approval_time');
