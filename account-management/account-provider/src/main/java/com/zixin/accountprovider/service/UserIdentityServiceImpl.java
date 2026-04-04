package com.zixin.accountprovider.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zixin.accountapi.api.UserIdentityAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.accountapi.po.User;
import com.zixin.accountapi.po.Doctor;
import com.zixin.accountapi.po.Patient;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.accountapi.vo.PatientVO;
import com.zixin.accountprovider.mapper.UserMapper;
import com.zixin.accountprovider.mapper.DoctorMapper;
import com.zixin.accountprovider.mapper.PatientMapper;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户身份信息服务实现
 * 
 * 提供医生、患者等不同身份用户的信息查询服务
 * 整合Account基础信息和身份扩展信息
 */
@Service
@DubboService
@Slf4j
public class UserIdentityServiceImpl implements UserIdentityAPI {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DoctorMapper doctorMapper;
    
    @Autowired
    private PatientMapper patientMapper;
    
    @Override
    public GetDoctorInfoResponse getDoctorInfo(GetDoctorInfoRequest request) {
        GetDoctorInfoResponse response = new GetDoctorInfoResponse();
        
        try {
            // 查询医生信息
            Doctor doctor = null;
            if (request.getDoctorId() != null) {
                doctor = doctorMapper.selectById(request.getDoctorId());
            } else if (request.getUserId() != null) {
                LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Doctor::getUserId, request.getUserId());
                doctor = doctorMapper.selectOne(wrapper);
            }
            
            if (doctor == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("医生不存在");
                return response;
            }
            
            // 查询账户信息
            User user = userMapper.selectById(doctor.getUserId());
            if (user == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("关联账户不存在");
                return response;
            }
            
            // 组装VO
            DoctorVO vo = new DoctorVO();
            BeanUtils.copyProperties(doctor, vo);
            BeanUtils.copyProperties(user, vo);
            vo.setUsername(user.getUsername());

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setDoctor(vo);
            
            log.info("Get doctor info success, doctorId: {}, userId: {}", 
                    doctor.getId(), doctor.getUserId());
        } catch (Exception e) {
            log.error("Get doctor info error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询医生信息异常: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public GetDoctorInfoResponse getDoctorInfoByUserId(Long userId) {
        return getDoctorInfo(GetDoctorInfoRequest
                .builder()
                .userId(userId)
                .build());
    }
    
    @Override
    public GetPatientInfoResponse getPatientInfo(GetPatientInfoRequest request) {
        GetPatientInfoResponse response = new GetPatientInfoResponse();
        
        try {
            // 查询患者信息
            Patient patient = null;
            if (request.getPatientId() != null) {
                patient = patientMapper.selectById(request.getPatientId());
            } else if (request.getUserId() != null) {
                LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Patient::getUserId, request.getUserId());
                patient = patientMapper.selectOne(wrapper);
            }
            
            if (patient == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("患者不存在, userid:" + request.getUserId());
                return response;
            }
            
            // 查询账户信息
            User user = userMapper.selectById(patient.getUserId());
            if (user == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("关联账户不存在");
                return response;
            }
            
            // 组装VO
            PatientVO vo = new PatientVO();
            BeanUtils.copyProperties(patient, vo);
            BeanUtils.copyProperties(user, vo);
            vo.setUsername(user.getUsername());
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setPatient(vo);
            
            log.info("Get patient info success, patientId: {}, userId: {}", 
                    patient.getId(), patient.getUserId());
        } catch (Exception e) {
            log.error("Get patient info error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询患者信息异常: " + e.getMessage());
        }
        
        return response;
    }
    
    @Override
    public GetPatientInfoResponse getPatientInfoByUserId(Long userId) {
        return getPatientInfo(GetPatientInfoRequest
                .builder()
                .userId(userId)
                .build());
    }

    @Override
    public GetMyPatientsResponse getMyPatients(GetMyPatientsRequest request) {
        // 1. 查询医生信息，验证医生身份
        DoctorVO doctor = getDoctorInfoByUserId(request.getDoctorId()).getDoctor();
        if (doctor == null) {
            log.error("Get my patients failed, doctor not found, userId: {}", request.getDoctorId());
            GetMyPatientsResponse response = new GetMyPatientsResponse();
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("医生不存在");
            return response;
        }

        // 2. 创建 MyBatis-Plus 的分页对象
        Page<Patient> page =
                new Page<>(
                        request.getPageNum(), request.getPageSize()
                );

        // 3. 构建查询条件
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patient::getAttendingDoctorId, request.getDoctorId())
                .orderByDesc(Patient::getCreateTime);
        // 根据患者姓名进行模糊匹配
        if(request.getPatientName() != null) {
            wrapper.like(Patient::getUpdateTime, request.getPatientName());
        }

        // 4. 执行分页查询
        Page<Patient> patientPage =
                patientMapper.selectPage(page, wrapper);

        // 5. 使用自定义 PageUtils 封装分页数据
        PageUtils pageUtils = new PageUtils(patientPage);

        // 6. 构建返回对象
        GetMyPatientsResponse response = new GetMyPatientsResponse();
        response.setCode(ToBCodeEnum.SUCCESS);
        response.setMessage("查询成功");

        // 7. 赋值
        response.setPatients(pageUtils);

        return response;
    }

    @Override
    public GetAllDoctorsResponse getAllDoctors() {
        GetAllDoctorsResponse response = new GetAllDoctorsResponse();

        try {
            // 查询所有医生
            LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(Doctor::getDepartment);
            List<Doctor> doctors = doctorMapper.selectList(wrapper);

            if (doctors == null || doctors.isEmpty()) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("暂无医生数据");
                response.setDoctors(new ArrayList<>());
                return response;
            }

            // 组装VO列表
            List<DoctorVO> doctorVOList = doctors.stream().map(doctor -> {
                User user = userMapper.selectById(doctor.getUserId());
                DoctorVO vo = new DoctorVO();
                BeanUtils.copyProperties(doctor, vo);
                if (user != null) {
                    BeanUtils.copyProperties(user, vo);
                    vo.setUsername(user.getUsername());
                }
                return vo;
            }).collect(Collectors.toList());

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            response.setDoctors(doctorVOList);

            log.info("Get all doctors success, count: {}", doctorVOList.size());
        } catch (Exception e) {
            log.error("Get all doctors error", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询医生列表异常: " + e.getMessage());
        }

        return response;
    }
}
