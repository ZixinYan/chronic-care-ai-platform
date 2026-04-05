package com.zixin.doctorconsumer.controller;

import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.GetAllDoctorsResponse;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctor/info")
@Slf4j
public class DoctorInfoController {

    @DubboReference(check = false)
    private UserIdentityAPI userIdentityAPI;

    @GetMapping("/list")
    public Result<List<DoctorVO>> getAllDoctors() {
        log.info("Get all doctors request");
        
        try {
            GetAllDoctorsResponse response = userIdentityAPI.getAllDoctors();
            
            if (response.getCode() != ToBCodeEnum.SUCCESS) {
                log.error("Get all doctors failed: {}", response.getMessage());
                return Result.error(response.getMessage());
            }
            
            List<DoctorVO> doctors = response.getDoctors();
            log.info("Get all doctors success, count: {}", doctors.size());
            return Result.success(doctors);
        } catch (Exception e) {
            log.error("Get all doctors failed", e);
            return Result.error("获取医生列表失败: " + e.getMessage());
        }
    }
}
