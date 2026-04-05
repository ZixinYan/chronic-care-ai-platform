package com.zixin.doctorprovider.client;

import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.accountapi.po.Doctor;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.PageUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorClient {
    @DubboReference(timeout = 50000)
    private UserIdentityAPI userIdentityAPI;

    public DoctorVO getDoctorInfo(GetDoctorInfoRequest request) {
        GetDoctorInfoResponse doctorInfoResponse = userIdentityAPI.getDoctorInfo(request);
        if (!doctorInfoResponse.getCode().equals(ToBCodeEnum.SUCCESS)) {
            throw new RuntimeException("Failed to get doctor info: " + doctorInfoResponse.getMessage());
        }
        return doctorInfoResponse.getDoctor();
    }

    public PatientVO getPatientInfo(GetPatientInfoRequest request) {
        GetPatientInfoResponse patientInfoResponse = userIdentityAPI.getPatientInfo(request);
        if (!patientInfoResponse.getCode().equals(ToBCodeEnum.SUCCESS)) {
            throw new RuntimeException("Failed to get patient info: " + patientInfoResponse.getMessage());
        }
        return patientInfoResponse.getPatient();
    }


    public PageUtils getPatientsByDoctorId(GetMyPatientsRequest request) {
        GetMyPatientsResponse response = userIdentityAPI.getMyPatients(request);
        if (!response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            throw new RuntimeException("Failed to get patients for doctorId " + request.getDoctorId() + ": " + response.getMessage());
        }
        return response.getPatients();
    }

    public List<DoctorVO> getAllDoctors() {
        GetAllDoctorsResponse response = userIdentityAPI.getAllDoctors();
        if (!response.getCode().equals(ToBCodeEnum.SUCCESS)) {
            throw new RuntimeException("Failed to get all doctors: " + response.getMessage());
        }
        return response.getDoctors();
    }
}
