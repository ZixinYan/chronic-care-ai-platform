package com.zixin.doctorapi.api;

import com.zixin.doctorapi.dto.*;

public interface DoctorWorkbenchAPI {
    QueryScheduleResponse querySchedule(QueryScheduleRequest request);
    
    GetScheduleDetailResponse getScheduleDetail(Long scheduleId, Long doctorId);
    
    CompleteScheduleResponse completeSchedule(CompleteScheduleRequest request);
    
    CancelScheduleResponse cancelSchedule(Long scheduleId, Long doctorId, String reason);
    
    UpdateScheduleStatusResponse updateScheduleStatus(Long scheduleId, Long doctorId, String status);

    AddScheduleResponse addSchedule(AddScheduleRequest request);

    GetPatientSchedulesResponse getPatientSchedules(GetPatientSchedulesRequest request);

    CheckScheduleConflictResponse checkScheduleConflict(CheckScheduleConflictRequest request);

    GetDoctorAvailableSlotsResponse getDoctorAvailableSlots(GetDoctorAvailableSlotsRequest request);

}
