package com.zixin.healthcenterprovider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zixin.healthcenterapi.po.AISummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI总结Mapper
 */
@Mapper
public interface AISummaryMapper extends BaseMapper<AISummary> {

    /**
     * 查询患者指定天数内的AI总结
     *
     * @param patientId 患者ID
     * @param startTime 开始时间戳
     * @return AI总结列表
     */
    @Select("SELECT * FROM care_platform_ai_summary WHERE patient_id = #{patientId} " +
            "AND create_time >= #{startTime} AND dele = 0 ORDER BY create_time DESC")
    List<AISummary> selectRecentByPatientId(@Param("patientId") Long patientId, 
                                             @Param("startTime") Long startTime);
}