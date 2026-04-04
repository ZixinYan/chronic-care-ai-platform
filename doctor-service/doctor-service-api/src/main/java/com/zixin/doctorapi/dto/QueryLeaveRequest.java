package com.zixin.doctorapi.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询请假单请求
 */
@Data
public class QueryLeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 医生账户ID（可选，为空时可按科室/全院维度扩展）
     */
    private Long doctorId;

    /**
     * 请假状态（可选）
     */
    private String status;

    /**
     * 开始日期（可选，YYYY-MM-DD）
     */
    private String startDay;

    /**
     * 结束日期（可选，YYYY-MM-DD）
     */
    private String endDay;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}

