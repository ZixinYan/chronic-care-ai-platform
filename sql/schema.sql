-- ============================================================
-- 慢病管理AI平台 - 完整数据库建表脚本
-- 生成时间: 2026-03-28
-- 说明: 本脚本包含所有PO类对应的数据库表结构
-- ============================================================

-- ============================================================
-- 第一部分: 账户管理模块 (account-management)
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户账户表 (care_platform_user)
-- 对应PO: com.zixin.accountapi.po.User
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_user`;
CREATE TABLE `care_platform_user` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID (主键)',
    `username` VARCHAR(64) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `gender` INT DEFAULT NULL COMMENT '性别 (0-未知, 1-男, 2-女)',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
    `phone` VARCHAR(500) DEFAULT NULL COMMENT '手机号(加密存储)',
    `phone_hash` VARCHAR(64) DEFAULT NULL COMMENT '手机号SHA256哈希值(用于登录查询)',
    `email` VARCHAR(500) DEFAULT NULL COMMENT '邮箱(加密存储)',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
    `birthday` BIGINT DEFAULT NULL COMMENT '生日(毫秒时间戳)',
    `id_card` VARCHAR(500) DEFAULT NULL COMMENT '身份证号(加密存储)',
    `id_card_hash` VARCHAR(64) DEFAULT NULL COMMENT '身份证号SHA256哈希值(用于登录查询)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `dele` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone_hash` (`phone_hash`),
    UNIQUE KEY `uk_id_card_hash` (`id_card_hash`),
    KEY `idx_dele` (`dele`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- ------------------------------------------------------------
-- 2. 医生信息表 (care_platform_doctor)
-- 对应PO: com.zixin.accountapi.po.Doctor
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_doctor`;
CREATE TABLE `care_platform_doctor` (
    `id` BIGINT NOT NULL COMMENT '医生ID (主键)',
    `user_id` BIGINT NOT NULL COMMENT '账户ID (关联care_platform_user表)',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名',
    `department` VARCHAR(64) DEFAULT NULL COMMENT '科室',
    `title` VARCHAR(64) DEFAULT NULL COMMENT '职称 (如主治医师、主任医师等)',
    `experience` INT DEFAULT NULL COMMENT '工作经验(年数)',
    `certification_number` VARCHAR(500) DEFAULT NULL COMMENT '执业证书编号(加密存储)',
    `education` VARCHAR(64) DEFAULT NULL COMMENT '学历',
    `bio` TEXT DEFAULT NULL COMMENT '简介',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `dele` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` VARCHAR(2000) DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_department` (`department`),
    KEY `idx_dele` (`dele`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生信息表';

-- ------------------------------------------------------------
-- 3. 患者信息表 (care_platform_patient)
-- 对应PO: com.zixin.accountapi.po.Patient
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_patient`;
CREATE TABLE `care_platform_patient` (
    `id` BIGINT NOT NULL COMMENT '患者ID (主键)',
    `user_id` BIGINT NOT NULL COMMENT '账户ID (关联care_platform_user表)',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '姓名',
    `attending_doctor_id` BIGINT DEFAULT NULL COMMENT '主治医生ID',
    `attending_doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名',
    `medical_history` TEXT DEFAULT NULL COMMENT '病史摘要',
    `allergies` VARCHAR(500) DEFAULT NULL COMMENT '过敏史',
    `blood_type` VARCHAR(10) DEFAULT NULL COMMENT '血型',
    `height` INT DEFAULT NULL COMMENT '身高(cm)',
    `weight` INT DEFAULT NULL COMMENT '体重(kg)',
    `emergency_contact` VARCHAR(64) DEFAULT NULL COMMENT '紧急联系人',
    `emergency_phone` VARCHAR(500) DEFAULT NULL COMMENT '紧急联系人电话(加密存储)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `dele` INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` VARCHAR(2000) DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_attending_doctor_id` (`attending_doctor_id`),
    KEY `idx_dele` (`dele`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表';

-- ------------------------------------------------------------
-- 4. 角色表 (care_platform_role)
-- 对应PO: com.zixin.accountapi.po.Role
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_role`;
CREATE TABLE `care_platform_role` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID (主键)',
    `code` INT NOT NULL COMMENT '角色代码 (1-DOCTOR, 2-PATIENT, 3-FAMILY, 4-ADMIN)',
    `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `action` INT DEFAULT NULL COMMENT '业务权限 (1-READ, 2-WRITE, 3-ALL)',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ------------------------------------------------------------
-- 5. 角色-权限关联表 (care_platform_role_permission)
-- 对应PO: com.zixin.accountapi.po.RolePermission
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_role_permission`;
CREATE TABLE `care_platform_role_permission` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `role_code` INT NOT NULL COMMENT '角色代码 (1-DOCTOR, 2-PATIENT, 3-FAMILY)',
    `action_code` INT NOT NULL COMMENT '权限代码 (1-READ, 2-WRITE, 3-ALL)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    PRIMARY KEY (`id`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- ------------------------------------------------------------
-- 6. 用户-角色关联表 (care_platform_user_role)
-- 对应PO: com.zixin.accountapi.po.UserRole
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `care_platform_user_role`;
CREATE TABLE `care_platform_user_role` (
    `user_role_id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_code` INT NOT NULL COMMENT '角色代码 (1-DOCTOR, 2-PATIENT, 3-FAMILY)',
    `create_time` BIGINT NOT NULL COMMENT '授权时间(毫秒时间戳)',
    PRIMARY KEY (`user_role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- ============================================================
-- 第二部分: 医生服务模块 (doctor-service)
-- ============================================================

-- ------------------------------------------------------------
-- 7. 医生日程表 (doctor_schedule)
-- 对应PO: com.zixin.doctorapi.po.DoctorSchedule
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `doctor_schedule`;
CREATE TABLE `doctor_schedule` (
    `id` BIGINT NOT NULL COMMENT '日程ID (主键)',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名(冗余字段)',
    `patient_id` BIGINT DEFAULT NULL COMMENT '患者ID',
    `patient_name` VARCHAR(64) DEFAULT NULL COMMENT '患者姓名(冗余字段)',
    `schedule` VARCHAR(500) DEFAULT NULL COMMENT '日程内容/描述',
    `schedule_category` VARCHAR(64) DEFAULT NULL COMMENT '日程类别',
    `schedule_day` VARCHAR(10) DEFAULT NULL COMMENT '日程日期 (格式: YYYY-MM-DD)',
    `priority` INT DEFAULT NULL COMMENT '优先级 (1-低, 2-中, 3-高, 4-紧急)',
    `status` VARCHAR(32) DEFAULT NULL COMMENT '日程状态 (PENDING/IN_PROGRESS/COMPLETED/CANCELLED)',
    `result` TEXT DEFAULT NULL COMMENT '执行结果/诊断报告',
    `link` VARCHAR(500) DEFAULT NULL COMMENT '关联链接',
    `start_time` BIGINT DEFAULT NULL COMMENT '日程开始时间(毫秒时间戳)',
    `end_time` BIGINT DEFAULT NULL COMMENT '日程结束时间(毫秒时间戳)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_schedule_day` (`schedule_day`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生日程表';

-- ------------------------------------------------------------
-- 8. 医生请假表 (doctor_leave)
-- 对应PO: com.zixin.doctorapi.po.DoctorLeave
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `doctor_leave`;
CREATE TABLE `doctor_leave` (
    `id` BIGINT NOT NULL COMMENT '请假单ID (主键)',
    `doctor_id` BIGINT NOT NULL COMMENT '医生账户ID',
    `doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名(冗余字段)',
    `leave_type` VARCHAR(32) NOT NULL COMMENT '请假类型 (SICK/ANNUAL/PERSONAL/TRAINING/OTHER)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '请假状态 (PENDING/APPROVED/REJECTED/CANCELLED)',
    `start_day` VARCHAR(10) NOT NULL COMMENT '请假开始日期 (格式: YYYY-MM-DD)',
    `end_day` VARCHAR(10) NOT NULL COMMENT '请假结束日期 (格式: YYYY-MM-DD)',
    `start_time` BIGINT DEFAULT NULL COMMENT '请假开始时间(毫秒时间戳,可选)',
    `end_time` BIGINT DEFAULT NULL COMMENT '请假结束时间(毫秒时间戳,可选)',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '请假原因',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_day` (`start_day`),
    KEY `idx_date_range` (`start_day`, `end_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生请假表';

-- ------------------------------------------------------------
-- 9. 电子病历表 (medical_record)
-- 对应PO: com.zixin.doctorapi.po.MedicalRecord
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
    `id` BIGINT NOT NULL COMMENT '病历ID (主键)',
    `schedule_id` BIGINT DEFAULT NULL COMMENT '关联日程ID',
    `doctor_id` BIGINT NOT NULL COMMENT '医生ID',
    `doctor_name` VARCHAR(64) DEFAULT NULL COMMENT '医生姓名(冗余字段)',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID',
    `patient_name` VARCHAR(64) DEFAULT NULL COMMENT '患者姓名(冗余字段)',
    `visit_date` VARCHAR(10) DEFAULT NULL COMMENT '就诊日期 (格式: YYYY-MM-DD)',
    `visit_type` VARCHAR(64) DEFAULT NULL COMMENT '就诊类型',
    `chief_complaint` TEXT DEFAULT NULL COMMENT '主诉',
    `present_illness` TEXT DEFAULT NULL COMMENT '现病史',
    `past_history` TEXT DEFAULT NULL COMMENT '既往史',
    `diagnosis` TEXT DEFAULT NULL COMMENT '诊断意见',
    `treatment_plan` TEXT DEFAULT NULL COMMENT '治疗建议',
    `prescription` TEXT DEFAULT NULL COMMENT '处方信息',
    `precautions` TEXT DEFAULT NULL COMMENT '注意事项',
    `follow_up_advice` TEXT DEFAULT NULL COMMENT '随访建议',
    `full_content` MEDIUMTEXT DEFAULT NULL COMMENT '完整病历内容(Markdown格式)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号(乐观锁)',
    `ext` JSON DEFAULT NULL COMMENT '扩展字段(JSON格式)',
    PRIMARY KEY (`id`),
    KEY `idx_schedule_id` (`schedule_id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子病历表';

-- ============================================================
-- 第三部分: 健康报告中心模块 (health-report-center)
-- ============================================================

-- ------------------------------------------------------------
-- 10. 健康报告表 (health_report)
-- 对应PO: com.zixin.healthcenterapi.po.HealthReport
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `health_report`;
CREATE TABLE `health_report` (
    `report_id` BIGINT NOT NULL COMMENT '报告ID (主键)',
    `patient_id` BIGINT NOT NULL COMMENT '患者ID (关联patient表)',
    `attending_doctor_id` BIGINT DEFAULT NULL COMMENT '主治医生ID (关联doctor表)',
    `report_type` INT NOT NULL COMMENT '报告类型 (1-IMAGE图片, 2-TEXT文字, 3-PDF)',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '报告分类 (体检报告/血液检查/影像检查等)',
    `title` VARCHAR(200) NOT NULL COMMENT '报告标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '报告描述/备注',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '报告文件URL (OSS存储路径)',
    `text_content` TEXT DEFAULT NULL COMMENT '文字内容 (文字类型报告)',
    `report_date` VARCHAR(10) DEFAULT NULL COMMENT '报告日期 (格式: YYYY-MM-DD)',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID (关联account表)',
    `uploader_name` VARCHAR(64) DEFAULT NULL COMMENT '上传者姓名',
    `hospital_name` VARCHAR(128) DEFAULT NULL COMMENT '医疗机构名称',
    `status` INT DEFAULT 0 COMMENT '审核状态 (0-PENDING待审核, 1-APPROVED已通过, 2-REJECTED已拒绝)',
    `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`report_id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_attending_doctor_id` (`attending_doctor_id`),
    KEY `idx_report_date` (`report_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康报告表';

-- ============================================================
-- 第四部分: 消息系统模块 (message-system)
-- ============================================================

-- ------------------------------------------------------------
-- 11. 站内信表 (message_inbox)
-- 对应PO: com.zixin.messageapi.po.Message
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `message_inbox`;
CREATE TABLE `message_inbox` (
    `message_id` BIGINT NOT NULL COMMENT '消息ID (主键)',
    `message_type` INT NOT NULL COMMENT '消息类型 (1-SYSTEM系统通知, 2-PERSONAL个人消息, 3-BROADCAST群发消息, 4-ANNOUNCEMENT公告)',
    `sender_id` BIGINT DEFAULT NULL COMMENT '发送者ID (系统消息时为null)',
    `sender_name` VARCHAR(64) DEFAULT NULL COMMENT '发送者名称',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `receiver_name` VARCHAR(64) DEFAULT NULL COMMENT '接收者名称',
    `group_message_id` BIGINT DEFAULT NULL COMMENT '群发消息组ID (群发消息时使用)',
    `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content` TEXT DEFAULT NULL COMMENT '消息内容',
    `status` INT NOT NULL DEFAULT 0 COMMENT '消息状态 (0-UNREAD未读, 1-READ已读, 2-REVOKED已撤回, 3-DELETED已删除)',
    `is_broadcast` INT NOT NULL DEFAULT 0 COMMENT '是否群发消息 (0-否, 1-是)',
    `read_time` BIGINT DEFAULT NULL COMMENT '阅读时间(毫秒时间戳)',
    `revoke_time` BIGINT DEFAULT NULL COMMENT '撤回时间(毫秒时间戳)',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    `deleted` INT NOT NULL DEFAULT 0 COMMENT '删除标记 (0-未删除, 1-已删除)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`message_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_receiver_id` (`receiver_id`),
    KEY `idx_group_message_id` (`group_message_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内信表';

-- ------------------------------------------------------------
-- 12. 消息模板表 (message_template)
-- 对应PO: com.zixin.messageapi.po.MessageTemplate
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `message_template`;
CREATE TABLE `message_template` (
    `template_id` BIGINT NOT NULL COMMENT '模板ID (主键)',
    `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码 (唯一标识)',
    `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `message_type` INT NOT NULL COMMENT '消息类型 (关联MessageType枚举)',
    `title_template` VARCHAR(200) DEFAULT NULL COMMENT '标题模板 (支持占位符{variableName})',
    `content_template` TEXT DEFAULT NULL COMMENT '内容模板 (支持占位符{variableName})',
    `params` VARCHAR(500) DEFAULT NULL COMMENT '参数列表 (JSON格式)',
    `is_enabled` INT NOT NULL DEFAULT 1 COMMENT '是否启用 (0-禁用, 1-启用)',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    `create_time` BIGINT NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT NOT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息模板表';

-- ============================================================
-- 第五部分: 初始数据
-- ============================================================

-- 初始化角色数据
INSERT INTO `care_platform_role` (`role_id`, `code`, `name`, `description`, `action`, `deleted`, `version`, `create_time`, `update_time`) VALUES
(1, 1, '医生', '医生角色', 3, 0, 0, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(2, 2, '患者', '患者角色', 2, 0, 0, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(3, 3, '家属', '家属角色', 1, 0, 0, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(4, 4, '管理员', '系统管理员', 3, 0, 0, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- 初始化角色权限数据
INSERT INTO `care_platform_role_permission` (`id`, `role_code`, `action_code`, `create_time`) VALUES
(1, 1, 3, UNIX_TIMESTAMP()*1000),
(2, 2, 2, UNIX_TIMESTAMP()*1000),
(3, 3, 1, UNIX_TIMESTAMP()*1000),
(4, 4, 3, UNIX_TIMESTAMP()*1000);

-- 初始化消息模板数据
INSERT INTO `message_template` (`template_id`, `template_code`, `template_name`, `message_type`, `title_template`, `content_template`, `params`, `is_enabled`, `remark`, `create_time`, `update_time`) VALUES
(1, 'HEALTH_ALERT', '健康预警通知', 1, '健康预警：{indicator}异常', '尊敬的{userName}，您的{indicator}数值为{value}，存在异常风险，请注意关注。', '["userName","indicator","value"]', 1, '健康指标异常预警', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(2, 'APPOINTMENT_REMIND', '预约提醒', 2, '预约提醒：{doctorName}', '您好，您已成功预约{doctorName}医生，就诊时间为{appointmentTime}，请准时到达。', '["doctorName","appointmentTime"]', 1, '预约成功提醒', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(3, 'REPORT_READY', '报告就绪通知', 1, '报告就绪：{reportTitle}', '您的{reportTitle}已生成，请登录系统查看详情。', '["reportTitle"]', 1, '检查报告完成通知', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(4, 'MEDICATION_REMIND', '服药提醒', 2, '服药提醒', '您好，请记得按时服用{medicationName}，剂量：{dosage}。', '["medicationName","dosage"]', 1, '定时服药提醒', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000);

-- ============================================================
-- 表结构说明
-- ============================================================
--
-- 1. 时间字段统一使用 BIGINT 类型存储毫秒时间戳
--    - 便于Java层面直接使用 Long 类型
--    - 避免时区问题
--
-- 2. 逻辑删除字段
--    - dele/deleted: 0-未删除, 1-已删除
--    - 使用MyBatis-Plus @TableLogic 注解
--
-- 3. 乐观锁字段
--    - version: 版本号
--    - 使用MyBatis-Plus @Version 注解
--
-- 4. 敏感字段加密
--    - phone, email, id_card, certification_number, emergency_phone
--    - 使用AES加密存储
--    - 配合 phone_hash, id_card_hash 哈希字段用于查询
--
-- 5. 扩展字段
--    - ext: JSON格式存储扩展信息
--    - 便于后续功能扩展无需修改表结构
--
-- ============================================================
