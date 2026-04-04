package com.zixin.doctorapi.po;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * 医生请假表
 *
 * 用于存储医生的请假信息，供排班和 AI 推荐时参考。
 */
@Data
@TableName("doctor_leave")
public class DoctorLeave {

    /**
     * 请假单ID (主键)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 医生账户ID
     */
    private Long doctorId;

    /**
     * 医生姓名（冗余字段，方便查询展示）
     */
    private String doctorName;

    /**
     * 请假类型（SICK/ANNUAL/PERSONAL/TRAINING/OTHER）
     */
    private String leaveType;

    /**
     * 请假状态（PENDING/APPROVED/REJECTED/CANCELLED）
     */
    private String status;

    /**
     * 请假开始日期 (格式: YYYY-MM-DD)
     */
    private String startDay;

    /**
     * 请假结束日期 (格式: YYYY-MM-DD)
     */
    private String endDay;

    /**
     * 请假开始时间（毫秒时间戳，可选）
     */
    private Long startTime;

    /**
     * 请假结束时间（毫秒时间戳，可选）
     */
    private Long endTime;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Integer version;

    /**
     * 扩展字段 (JSON格式)
     */
    private JSON ext;
}

