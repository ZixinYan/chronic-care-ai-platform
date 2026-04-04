package com.zixin.healthcenterapi.api;

import com.zixin.healthcenterapi.dto.*;

/**
 * 健康报告中心 Dubbo API
 *
 * 提供健康报告的上传、查询等核心功能
 *
 * @author zixin
 */
public interface HealthReportAPI {

    /**
     * 上传健康报告
     *
     * 功能说明:
     * 1. 支持图片、PDF、文字三种类型报告
     * 2. 图片/PDF类型需要先通过OSS上传文件
     * 3. 自动关联患者的主治医生
     * 4. 报告默认状态为待审核
     *
     * 权限要求:
     * - 患者本人可上传
     * - 主治医生可代为上传
     *
     * @param request 上传请求
     * @return 上传响应(包含reportId和fileUrl)
     */
    UploadReportResponse uploadReport(UploadReportRequest request);

    /**
     * 获取最近N次健康报告
     *
     * 功能说明:
     * 1. 按创建时间倒序获取最近N条报告
     * 2. 支持图片、PDF、文字所有类型
     *
     * 权限控制:
     * - 患者只能查看自己的报告
     * - 医生只能查看自己患者的报告
     *
     * @param request 查询请求
     * @return 报告列表
     */
    GetRecentReportsResponse getRecentReports(GetRecentReportsRequest request);

    /**
     * 获取最近N天的AI总结信息
     *
     * 功能说明:
     * 1. 获取指定天数内的AI生成报告总结
     * 2. 按日期倒序排列
     *
     * 权限控制:
     * - 患者只能查看自己的总结
     * - 医生只能查看自己患者的总结
     *
     * @param request 查询请求
     * @return AI总结列表
     */
    GetRecentAISummaryResponse getRecentAISummary(GetRecentAISummaryRequest request);

    /**
     * 生成AI健康报告并自动添加到报告库
     *
     * 功能说明:
     * 1. 基于血糖预测数据调用AI能力生成报告
     * 2. 自动保存到患者报告库
     * 3. 如果血糖超阈值则触发预警
     *
     * @param request 生成请求
     * @return 生成结果
     */
    GenerateAIReportResponse generateAIReport(GenerateAIReportRequest request);
    
    /**
     * 查询健康报告列表
     * 
     * 功能说明:
     * 1. 支持按患者ID查询
     * 2. 支持按报告类型、分类、状态筛选
     * 3. 分页查询
     * 
     * 权限控制:
     * - 患者只能查看自己的报告
     * - 医生只能查看自己患者的报告
     * - 管理员可查看所有报告
     * 
     * @param request 查询请求
     * @return 查询响应(包含报告列表和分页信息)
     */
    QueryReportListResponse queryReportList(QueryReportListRequest request);
    
    /**
     * 获取报告详情
     * 
     * 功能说明:
     * 1. 根据reportId获取完整报告信息
     * 2. 包含患者和医生的基本信息
     * 
     * 权限控制:
     * - 患者只能查看自己的报告
     * - 医生只能查看自己患者的报告
     * 
     * @param request 获取详情请求
     * @return 报告详情响应
     */
    GetReportDetailResponse getReportDetail(GetReportDetailRequest request);

    /**
     * 处理健康报告
     *
     * 功能说明:
     * 1. 医生对患者上传的健康报告进行审核和处理
     * 2. 可以添加处理意见和建议 auditMark
     * 3. 更新报告状态（如：待处理、已处理、驳回等） status
     *
     * 权限控制:
     * - 只有主治医生可以处理对应患者的报告
     *
     * @param request 处理请求
     * @return 处理响应(包含处理结果和更新后的报告状态)
     */
    ProcessReportResponse processReport(ProcessReportRequest request);

    /**
     * 检查血糖阈值并发送预警短信
     *
     * 功能说明:
     * 1. 检查CBG是否超过阈值
     * 2. 如果超过阈值，查询患者家属信息
     * 3. 发送预警短信给家属
     *
     * 阈值规则:
     * - 空腹 > 8.3 mmol/L
     * - 餐后1h > 12.7 mmol/L
     * - 餐后2h > 11.1 mmol/L
     * - 餐后3h > 10.0 mmol/L
     *
     * @param request 检查请求
     * @return 检查结果
     */
    CheckGlucoseAlertResponse checkGlucoseAlert(CheckGlucoseAlertRequest request);

    /**
     * 保存文字报告（轻量版，仅入库，不触发 AI 排班）
     *
     * 功能说明:
     * 1. 仅支持文字类型报告入库
     * 2. 不触发 AI 排班和医生审核流程
     * 3. 适用于系统自动生成的报告（如血糖预测报告）
     *
     * @param request 保存请求
     * @return 保存结果（包含 reportId）
     */
    SaveTextReportResponse saveTextReport(SaveTextReportRequest request);
}
