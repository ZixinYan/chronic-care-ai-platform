-- 医生休假审批功能数据库更新脚本
-- 为doctor_leave表添加审批相关字段

USE `chronic-care-ai-platform`;

-- 添加审批人ID字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approver_id` BIGINT(20) DEFAULT NULL COMMENT '审批人ID' AFTER `reason`;

-- 添加审批人姓名字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approver_name` VARCHAR(100) DEFAULT NULL COMMENT '审批人姓名' AFTER `approver_id`;

-- 添加审批意见字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approval_comment` TEXT DEFAULT NULL COMMENT '审批意见' AFTER `approver_name`;

-- 添加审批时间字段
ALTER TABLE `doctor_leave` 
ADD COLUMN `approval_time` BIGINT(20) DEFAULT NULL COMMENT '审批时间（毫秒时间戳）' AFTER `approval_comment`;

-- 添加索引以优化查询性能
ALTER TABLE `doctor_leave` 
ADD INDEX `idx_status` (`status`),
ADD INDEX `idx_approver_id` (`approver_id`);
