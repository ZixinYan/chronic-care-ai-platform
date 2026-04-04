package com.zixin.doctorprovider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zixin.doctorapi.po.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子病历 Mapper
 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {
}
