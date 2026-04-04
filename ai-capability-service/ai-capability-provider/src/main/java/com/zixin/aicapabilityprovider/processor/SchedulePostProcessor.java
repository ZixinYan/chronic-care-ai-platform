package com.zixin.aicapabilityprovider.processor;

import com.zixin.aicapabilityapi.vo.SuggestScheduleVO;
import com.zixin.aicapabilityprovider.constants.ScheduleConstants;
import com.zixin.aicapabilityprovider.prompt.SchedulePromptBuilder.DoctorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public final class SchedulePostProcessor {

    private SchedulePostProcessor() {}

    public static void fillMissingFields(
            List<SuggestScheduleVO> schedules,
            String scheduleDay,
            Map<Long, DoctorContext> doctorContextMap) {

        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        for (SuggestScheduleVO schedule : schedules) {
            if (schedule == null) continue;

            fillDoctorInfo(schedule, doctorContextMap);
            fillScheduleDay(schedule, scheduleDay);
            fillStatusFields(schedule);
            fillPriorityFields(schedule);
            fillCategoryFields(schedule);
            fillTimeFields(schedule);
        }
    }

    private static void fillDoctorInfo(SuggestScheduleVO schedule, Map<Long, DoctorContext> doctorContextMap) {
        if (doctorContextMap == null || doctorContextMap.isEmpty()) {
            return;
        }

        Long doctorId = schedule.getDoctorId();
        if (doctorId == null) {
            log.warn("Schedule missing doctorId, cannot fill doctor info");
            return;
        }

        DoctorContext ctx = doctorContextMap.get(doctorId);
        if (ctx == null) {
            log.warn("DoctorContext not found for doctorId: {}", doctorId);
            return;
        }

        if (schedule.getDoctorName() == null || schedule.getDoctorName().isEmpty()) {
            schedule.setDoctorName(ctx.getDoctorName());
            log.debug("Filled doctorName for doctorId {}: {}", doctorId, ctx.getDoctorName());
        }
    }

    private static void fillScheduleDay(SuggestScheduleVO schedule, String scheduleDay) {
        if ((schedule.getScheduleDay() == null || schedule.getScheduleDay().isEmpty()) 
                && scheduleDay != null && !scheduleDay.isEmpty()) {
            schedule.setScheduleDay(scheduleDay);
            log.debug("Filled scheduleDay: {}", scheduleDay);
        }
    }

    private static void fillStatusFields(SuggestScheduleVO schedule) {
        if (schedule.getStatus() == null || schedule.getStatus().isEmpty()) {
            schedule.setStatus(ScheduleConstants.DefaultValues.DEFAULT_STATUS);
            log.debug("Filled default status: {}", ScheduleConstants.DefaultValues.DEFAULT_STATUS);
        }

        if (schedule.getStatusDesc() == null || schedule.getStatusDesc().isEmpty()) {
            schedule.setStatusDesc(ScheduleConstants.Status.getDesc(schedule.getStatus()));
            log.debug("Filled statusDesc: {}", schedule.getStatusDesc());
        }
    }

    private static void fillPriorityFields(SuggestScheduleVO schedule) {
        if (schedule.getPriority() == null) {
            schedule.setPriority(ScheduleConstants.DefaultValues.DEFAULT_PRIORITY);
            log.debug("Filled default priority: {}", ScheduleConstants.DefaultValues.DEFAULT_PRIORITY);
        }

        if (schedule.getPriorityDesc() == null || schedule.getPriorityDesc().isEmpty()) {
            schedule.setPriorityDesc(ScheduleConstants.Priority.getDesc(schedule.getPriority()));
            log.debug("Filled priorityDesc: {}", schedule.getPriorityDesc());
        }
    }

    private static void fillCategoryFields(SuggestScheduleVO schedule) {
        if (schedule.getScheduleCategory() == null) {
            schedule.setScheduleCategory(ScheduleConstants.DefaultValues.DEFAULT_CATEGORY);
            log.debug("Filled default scheduleCategory: {}", ScheduleConstants.DefaultValues.DEFAULT_CATEGORY);
        }

        if (schedule.getScheduleCategoryName() == null || schedule.getScheduleCategoryName().isEmpty()) {
            schedule.setScheduleCategoryName(ScheduleConstants.ScheduleCategory.getDesc(schedule.getScheduleCategory()));
            log.debug("Filled scheduleCategoryName: {}", schedule.getScheduleCategoryName());
        }
    }

    private static void fillTimeFields(SuggestScheduleVO schedule) {
        if (schedule.getStartTime() == null) {
            schedule.setStartTime(ScheduleConstants.DefaultValues.DEFAULT_START_TIME);
        }
        if (schedule.getEndTime() == null) {
            schedule.setEndTime(ScheduleConstants.DefaultValues.DEFAULT_END_TIME);
        }
    }

    public static boolean validateSchedule(SuggestScheduleVO schedule) {
        if (schedule == null) {
            return false;
        }

        boolean valid = true;

        if (schedule.getDoctorId() == null) {
            log.warn("Validation failed: doctorId is null");
            valid = false;
        }

        if (schedule.getSchedule() == null || schedule.getSchedule().isEmpty()) {
            log.warn("Validation failed: schedule content is empty");
            valid = false;
        }

        if (schedule.getScheduleDay() == null || schedule.getScheduleDay().isEmpty()) {
            log.warn("Validation failed: scheduleDay is empty");
            valid = false;
        }

        return valid;
    }

    public static int removeInvalidSchedules(List<SuggestScheduleVO> schedules) {
        if (schedules == null) {
            return 0;
        }

        int originalSize = schedules.size();
        schedules.removeIf(schedule -> !validateSchedule(schedule));
        int removed = originalSize - schedules.size();

        if (removed > 0) {
            log.info("Removed {} invalid schedules", removed);
        }

        return removed;
    }
}
