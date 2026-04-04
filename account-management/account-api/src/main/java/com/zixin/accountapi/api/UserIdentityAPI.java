package com.zixin.accountapi.api;

import com.zixin.accountapi.dto.*;

/**
 * 用户身份信息服务API
 * 
 * 提供医生、患者等不同身份用户的信息查询服务
 * 所有跟账号身份相关的服务都在account-management中
 */
public interface UserIdentityAPI {
    
    /**
     * 获取医生信息
     * 
     * 整合Account基础信息和Doctor扩展信息
     *
     * @param request 请求参数
     * @return 医生完整信息
     */
    GetDoctorInfoResponse getDoctorInfo(GetDoctorInfoRequest request);
    
    /**
     * 根据账户ID获取医生信息
     *
     * @param userId 账户ID
     * @return 医生完整信息
     */
    GetDoctorInfoResponse getDoctorInfoByUserId(Long userId);
    
    /**
     * 获取患者信息
     * 
     * 整合Account基础信息和Patient扩展信息
     *
     * @param request 请求参数
     * @return 患者完整信息
     */
    GetPatientInfoResponse getPatientInfo(GetPatientInfoRequest request);
    
    /**
     * 根据账户ID获取患者信息
     *
     * @param userId 账户ID
     * @return 患者完整信息
     */
    GetPatientInfoResponse getPatientInfoByUserId(Long userId);


    GetMyPatientsResponse getMyPatients(GetMyPatientsRequest request);

    /**
     * 获取所有医生信息
     *
     * @return 所有医生列表
     */
    GetAllDoctorsResponse getAllDoctors();
}
