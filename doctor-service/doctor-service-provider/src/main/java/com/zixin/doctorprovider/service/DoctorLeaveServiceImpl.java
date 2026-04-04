package com.zixin.doctorprovider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zixin.accountapi.dto.GetDoctorInfoRequest;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.doctorapi.api.DoctorLeaveAPI;
import com.zixin.doctorapi.dto.*;
import com.zixin.doctorapi.enums.LeaveStatus;
import com.zixin.doctorapi.enums.LeaveType;
import com.zixin.doctorapi.po.DoctorLeave;
import com.zixin.doctorapi.vo.DoctorLeaveVO;
import com.zixin.doctorprovider.client.DoctorClient;
import com.zixin.doctorprovider.mapper.DoctorLeaveMapper;
import com.zixin.utils.exception.ToBCodeEnum;
import com.zixin.utils.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 医生请假服务实现
 */
@Service
@DubboService
@Slf4j
public class DoctorLeaveServiceImpl implements DoctorLeaveAPI {

    private final DoctorLeaveMapper leaveMapper;
    private final DoctorClient doctorClient;

    public DoctorLeaveServiceImpl(DoctorLeaveMapper leaveMapper, DoctorClient doctorClient) {
        this.leaveMapper = leaveMapper;
        this.doctorClient = doctorClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddLeaveResponse addLeave(AddLeaveRequest request) {
        AddLeaveResponse response = new AddLeaveResponse();

        try {
            if (request.getDoctorId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("医生ID不能为空");
                return response;
            }

            // 构建实体
            DoctorLeave leave = new DoctorLeave();
            leave.setDoctorId(request.getDoctorId());
            leave.setLeaveType(request.getLeaveType());
            leave.setStartDay(request.getStartDay());
            leave.setEndDay(request.getEndDay());
            leave.setStartTime(request.getStartTime());
            leave.setEndTime(request.getEndTime());
            leave.setReason(request.getReason());
            leave.setStatus(LeaveStatus.PENDING.getCode());
            long now = System.currentTimeMillis();
            leave.setCreateTime(now);
            leave.setUpdateTime(now);

            // 补充医生姓名
            try {
                DoctorVO doctorVO = doctorClient.getDoctorInfo(GetDoctorInfoRequest.builder()
                        .userId(request.getDoctorId())
                        .build());
                if (doctorVO != null) {
                    leave.setDoctorName(doctorVO.getUsername());
                }
            } catch (Exception e) {
                log.warn("Failed to load doctor info when adding leave, doctorId={}", request.getDoctorId(), e);
            }

            int rows = leaveMapper.insert(leave);
            if (rows > 0) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("新增请假单成功");
                response.setLeaveId(leave.getId());
            } else {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("新增请假单失败");
            }
        } catch (Exception e) {
            log.error("Add leave failed, doctorId={}", request.getDoctorId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("新增请假单异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UpdateLeaveResponse updateLeave(UpdateLeaveRequest request) {
        UpdateLeaveResponse response = new UpdateLeaveResponse();

        try {
            if (request.getLeaveId() == null || request.getDoctorId() == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("请假单ID和医生ID不能为空");
                return response;
            }

            DoctorLeave dbLeave = leaveMapper.selectById(request.getLeaveId());
            if (dbLeave == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("请假单不存在");
                return response;
            }

            if (!dbLeave.getDoctorId().equals(request.getDoctorId())) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权操作该请假单");
                return response;
            }

            LambdaUpdateWrapper<DoctorLeave> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(DoctorLeave::getId, request.getLeaveId())
                    .eq(DoctorLeave::getVersion, dbLeave.getVersion());

            if (request.getLeaveType() != null) {
                wrapper.set(DoctorLeave::getLeaveType, request.getLeaveType());
            }
            if (request.getStartDay() != null) {
                wrapper.set(DoctorLeave::getStartDay, request.getStartDay());
            }
            if (request.getEndDay() != null) {
                wrapper.set(DoctorLeave::getEndDay, request.getEndDay());
            }
            if (request.getStartTime() != null) {
                wrapper.set(DoctorLeave::getStartTime, request.getStartTime());
            }
            if (request.getEndTime() != null) {
                wrapper.set(DoctorLeave::getEndTime, request.getEndTime());
            }
            if (request.getReason() != null) {
                wrapper.set(DoctorLeave::getReason, request.getReason());
            }
            if (request.getStatus() != null) {
                wrapper.set(DoctorLeave::getStatus, request.getStatus());
            }

            wrapper.set(DoctorLeave::getUpdateTime, System.currentTimeMillis());

            int rows = leaveMapper.update(null, wrapper);
            if (rows > 0) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("更新请假单成功");
            } else {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("更新请假单失败,请刷新后重试");
            }
        } catch (Exception e) {
            log.error("Update leave failed, leaveId={}, doctorId={}", request.getLeaveId(), request.getDoctorId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("更新请假单异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteLeaveResponse deleteLeave(Long leaveId, Long doctorId) {
        DeleteLeaveResponse response = new DeleteLeaveResponse();

        try {
            if (leaveId == null || doctorId == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("请假单ID和医生ID不能为空");
                return response;
            }

            DoctorLeave dbLeave = leaveMapper.selectById(leaveId);
            if (dbLeave == null) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("请假单不存在");
                return response;
            }

            if (!dbLeave.getDoctorId().equals(doctorId)) {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("无权操作该请假单");
                return response;
            }

            int rows = leaveMapper.deleteById(leaveId);
            if (rows > 0) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("删除请假单成功");
            } else {
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("删除请假单失败");
            }
        } catch (Exception e) {
            log.error("Delete leave failed, leaveId={}, doctorId={}", leaveId, doctorId, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("删除请假单异常: " + e.getMessage());
        }

        return response;
    }

    @Override
    public QueryLeaveResponse queryLeaves(QueryLeaveRequest request) {
        QueryLeaveResponse response = new QueryLeaveResponse();

        try {
            Page<DoctorLeave> page = new Page<>(request.getPageNum(), request.getPageSize());

            LambdaQueryWrapper<DoctorLeave> wrapper = new LambdaQueryWrapper<>();
            if (request.getDoctorId() != null) {
                wrapper.eq(DoctorLeave::getDoctorId, request.getDoctorId());
            }
            if (request.getStatus() != null) {
                wrapper.eq(DoctorLeave::getStatus, request.getStatus());
            }
            if (request.getStartDay() != null) {
                wrapper.ge(DoctorLeave::getStartDay, request.getStartDay());
            }
            if (request.getEndDay() != null) {
                wrapper.le(DoctorLeave::getEndDay, request.getEndDay());
            }

            wrapper.orderByDesc(DoctorLeave::getStartDay)
                    .orderByDesc(DoctorLeave::getCreateTime);

            Page<DoctorLeave> leavePage = leaveMapper.selectPage(page, wrapper);

            PageUtils pageUtils = new PageUtils(leavePage);
            if (pageUtils.getList() != null && !pageUtils.getList().isEmpty()) {
                @SuppressWarnings("unchecked")
                List<DoctorLeave> list = (List<DoctorLeave>) pageUtils.getList();
                List<DoctorLeaveVO> vos = list.stream()
                        .map(this::convertToVO)
                        .collect(Collectors.toList());
                pageUtils.setList(vos);
            }

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询请假单成功");
            response.setLeaves(pageUtils);
        } catch (Exception e) {
            log.error("Query leaves failed, request={}", request, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询请假单异常: " + e.getMessage());
        }

        return response;
    }

    private DoctorLeaveVO convertToVO(DoctorLeave leave) {
        DoctorLeaveVO vo = new DoctorLeaveVO();
        BeanUtils.copyProperties(leave, vo);

        // 类型描述
        try {
            if (leave.getLeaveType() != null) {
                LeaveType type = LeaveType.fromCode(leave.getLeaveType());
                vo.setLeaveTypeDesc(type.getDescription());
            }
        } catch (Exception e) {
            vo.setLeaveTypeDesc("未知");
        }

        // 状态描述
        try {
            if (leave.getStatus() != null) {
                LeaveStatus status = LeaveStatus.fromCode(leave.getStatus());
                vo.setStatusDesc(status.getDescription());
            }
        } catch (Exception e) {
            vo.setStatusDesc("未知");
        }

        return vo;
    }
}

