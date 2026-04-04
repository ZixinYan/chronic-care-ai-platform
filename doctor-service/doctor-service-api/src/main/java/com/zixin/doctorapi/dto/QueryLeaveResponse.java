package com.zixin.doctorapi.dto;

import com.zixin.utils.utils.BaseResponse;
import com.zixin.utils.utils.PageUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请假单响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryLeaveResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 请假单分页列表
     */
    private PageUtils leaves;
}

