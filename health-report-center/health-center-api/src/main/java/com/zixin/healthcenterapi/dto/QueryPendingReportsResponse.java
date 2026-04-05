package com.zixin.healthcenterapi.dto;

import com.zixin.healthcenterapi.vo.HealthReportVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 医生查询待审批报告响应
 * 
 * @author zixin
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryPendingReportsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 报告列表
     */
    private List<HealthReportVO> reportList;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
}
