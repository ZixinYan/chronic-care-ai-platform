package com.zixin.doctorprovider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zixin.doctorapi.po.DoctorSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<DoctorSchedule> list);
    /**
     * 批量插入或更新（主键冲突时更新）
     */
    int batchInsertOrUpdate(@Param("list") List<DoctorSchedule> list);
    /**
     * 批量插入（指定批次大小）
     */
    int batchInsertWithBatchSize(@Param("list") List<DoctorSchedule> list);

    /**
     * 查询医生在指定时间段内有冲突的日程
     * 冲突条件: 新日程开始时间 < 已有日程结束时间 AND 新日程结束时间 > 已有日程开始时间
     * 排除已取消的日程
     *
     * @param doctorId 医生ID
     * @param scheduleDay 日程日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeScheduleId 排除的日程ID（用于更新时排除自身）
     * @return 冲突的日程列表
     */
    List<DoctorSchedule> findConflictingSchedules(@Param("doctorId") Long doctorId,
                                                   @Param("scheduleDay") String scheduleDay,
                                                   @Param("startTime") Long startTime,
                                                   @Param("endTime") Long endTime,
                                                   @Param("excludeScheduleId") Long excludeScheduleId);

    /**
     * 查询医生在指定日期的所有有效日程（排除已取消）
     *
     * @param doctorId 医生ID
     * @param scheduleDay 日程日期
     * @return 日程列表
     */
    List<DoctorSchedule> findDoctorSchedulesByDay(@Param("doctorId") Long doctorId,
                                                   @Param("scheduleDay") String scheduleDay);
}