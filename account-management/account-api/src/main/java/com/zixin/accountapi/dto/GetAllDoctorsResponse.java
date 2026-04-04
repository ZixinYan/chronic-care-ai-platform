package com.zixin.accountapi.dto;

import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.utils.utils.BaseResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 获取所有医生响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetAllDoctorsResponse extends BaseResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 医生列表
     */
    private List<DoctorVO> doctors;
}
