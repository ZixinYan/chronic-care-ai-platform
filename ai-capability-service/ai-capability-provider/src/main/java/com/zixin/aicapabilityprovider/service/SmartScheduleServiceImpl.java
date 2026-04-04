package com.zixin.aicapabilityprovider.service;

import com.zixin.aicapabilityapi.api.AIScheduleAPI;
import com.zixin.aicapabilityapi.dto.AiScheduleResult;
import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityapi.dto.GenerateScheduleResponse;
import com.zixin.aicapabilityapi.vo.SuggestScheduleVO;
import com.zixin.aicapabilityprovider.processor.SchedulePostProcessor;
import com.zixin.aicapabilityprovider.prompt.SchedulePromptBuilder;
import com.zixin.aicapabilityprovider.prompt.SchedulePromptBuilder.DoctorContext;
import com.zixin.aicapabilityprovider.tool.DoctorScheduleTools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.zixin.utils.exception.ToBCodeEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@DubboService
public class SmartScheduleServiceImpl implements AIScheduleAPI {

    private static final int MAX_DETAIL_LOOKUPS = 3;

    private final ChatClient chatClient;
    private final DoctorScheduleTools doctorScheduleTools;
    private final Gson gson = new GsonBuilder().create();

    public SmartScheduleServiceImpl(
            @Qualifier("scheduleChatClient") ChatClient chatClient,
            DoctorScheduleTools doctorScheduleTools) {
        this.chatClient = chatClient;
        this.doctorScheduleTools = doctorScheduleTools;
    }

    @Override
    public GenerateScheduleResponse SmartScheduleGenerate(GenerateScheduleRequest request) {
        GenerateScheduleResponse response = new GenerateScheduleResponse();

        if (request == null || request.getScheduleDay() == null || request.getScheduleDay().isEmpty()) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("预约日期不可为空");
            response.setRecommendedSchedules(Collections.emptyList());
            response.setRecommendation("请求参数不完整，缺少 scheduleDay");
            return response;
        }

        String toolScheduleDay = normalizeScheduleDayForDoctorDb(request.getScheduleDay());

        log.info("[ReAct] Step 1: 查询所有医生信息");
        String allDoctorsObservation = doctorScheduleTools.queryAllDoctors();
        logObservation("allDoctors", allDoctorsObservation);

        log.info("[ReAct] Step 2: 查询医生请假情况");
        String leaveStartDay = calculateLeaveStartDay(toolScheduleDay, -7);
        String leaveEndDay = calculateLeaveStartDay(toolScheduleDay, 7);
        String leavesObservation = doctorScheduleTools.queryDoctorLeaves(leaveStartDay, leaveEndDay);
        logObservation("leaves", leavesObservation);

        log.info("[ReAct] Step 3: 查询医生排班情况");
        String availabilityObservation = doctorScheduleTools.queryDoctorAvailabilityForDay(
                toolScheduleDay,
                request.getBusinessRequirement() != null ? request.getBusinessRequirement() : ""
        );
        logObservation("availability", availabilityObservation);

        Map<Long, DoctorContext> doctorContextMap = extractDoctorContexts(availabilityObservation);

        log.info("[ReAct] Step 4: 查询候选医生的详细排班信息");
        List<String> detailObservations = new ArrayList<>();
        List<Long> candidateDoctorIds = extractTopDoctorIds(availabilityObservation, MAX_DETAIL_LOOKUPS);
        
        if (Boolean.TRUE.equals(request.getSpecifyDoctor()) && request.getDoctorId() != null) {
            if (!candidateDoctorIds.contains(request.getDoctorId())) {
                candidateDoctorIds.add(0, request.getDoctorId());
            }
        }

        for (Long doctorId : candidateDoctorIds) {
            String detailJson = doctorScheduleTools.queryDoctorScheduleDetailForDay(doctorId, toolScheduleDay);
            detailObservations.add(detailJson);
            logObservation("detail doctorId=" + doctorId, detailJson);
        }

        String userPrompt = SchedulePromptBuilder.buildUserPrompt(
                request, toolScheduleDay,
                allDoctorsObservation, leavesObservation, availabilityObservation, detailObservations,
                doctorContextMap
        );

        log.info("[ReAct] Step 5: 调用 AI 模型生成推荐");
        String modelResponse;
        try {
            modelResponse = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("modelResponse={}", modelResponse);
        } catch (Exception e) {
            log.error("AI model call failed", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("调用 AI 智能排班失败: " + e.getMessage());
            response.setRecommendedSchedules(Collections.emptyList());
            response.setRecommendation("AI 调用异常");
            return response;
        }

        String jsonPayload = extractJson(modelResponse);
        log.info("AI response JSON: {}", jsonPayload);

        List<SuggestScheduleVO> schedules = Collections.emptyList();
        String recommendation = null;
        try {
            AiScheduleResult result = gson.fromJson(jsonPayload, AiScheduleResult.class);
            if (result != null && result.getSchedules() != null) {
                schedules = result.getSchedules();
            }
            recommendation = result != null ? result.getRecommendation() : null;
        } catch (Exception e) {
            log.error("Failed to parse AI response JSON", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("解析 AI 排班 JSON 失败: " + e.getMessage());
            response.setRecommendedSchedules(Collections.emptyList());
            response.setRecommendation(modelResponse);
            return response;
        }

        if (schedules == null) {
            schedules = new ArrayList<>();
        }

        log.info("[ReAct] Step 6: 后处理 - 填充缺失字段");
        SchedulePostProcessor.fillMissingFields(schedules, toolScheduleDay, doctorContextMap);

        int removed = SchedulePostProcessor.removeInvalidSchedules(schedules);
        if (removed > 0) {
            log.warn("Removed {} invalid schedules from AI response", removed);
        }

        if (schedules.isEmpty()) {
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("AI 未返回有效推荐：所有日程均缺少必要字段");
            response.setRecommendedSchedules(schedules);
            response.setRecommendation(
                    recommendation == null || recommendation.isEmpty() ? jsonPayload : recommendation
            );
            return response;
        }

        response.setRecommendedSchedules(schedules);
        response.setRecommendation(
                recommendation == null || recommendation.isEmpty()
                        ? "AI 已生成智能排班结果"
                        : recommendation
        );
        response.setCode(ToBCodeEnum.SUCCESS);
        response.setMessage("AI 智能日程推荐成功");

        logScheduleResult(schedules);

        return response;
    }

    private void logObservation(String name, String observation) {
        log.debug("[ReAct] Observation {} (truncated): {}",
                name,
                observation.length() > 500 ? observation.substring(0, 500) + "..." : observation);
    }

    private void logScheduleResult(List<SuggestScheduleVO> schedules) {
        for (int i = 0; i < schedules.size(); i++) {
            SuggestScheduleVO s = schedules.get(i);
            log.info("Schedule[{}]: doctorId={}, doctorName={}, schedule={}, category={}, priority={}, status={}",
                    i, s.getDoctorId(), s.getDoctorName(), s.getSchedule(),
                    s.getScheduleCategoryName(), s.getPriorityDesc(), s.getStatusDesc());
        }
    }

    private String calculateLeaveStartDay(String scheduleDay, int offsetDays) {
        if (scheduleDay == null || scheduleDay.length() != 10) {
            return scheduleDay;
        }
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(scheduleDay);
            java.time.LocalDate resultDate = date.plusDays(offsetDays);
            return resultDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return scheduleDay;
        }
    }

    private Map<Long, DoctorContext> extractDoctorContexts(String availabilityJson) {
        Map<Long, DoctorContext> contextMap = new HashMap<>();
        if (availabilityJson == null || availabilityJson.isEmpty()) {
            return contextMap;
        }
        try {
            JsonObject root = JsonParser.parseString(availabilityJson).getAsJsonObject();
            if (root.has("error")) {
                return contextMap;
            }
            JsonArray doctors = root.getAsJsonArray("doctors");
            if (doctors == null) {
                return contextMap;
            }
            for (JsonElement el : doctors) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject d = el.getAsJsonObject();
                if (d.has("doctorId") && !d.get("doctorId").isJsonNull()) {
                    Long doctorId = d.get("doctorId").getAsLong();
                    String doctorName = d.has("doctorName") && !d.get("doctorName").isJsonNull()
                            ? d.get("doctorName").getAsString() : "";
                    String department = d.has("department") && !d.get("department").isJsonNull()
                            ? d.get("department").getAsString() : "";
                    String title = d.has("title") && !d.get("title").isJsonNull()
                            ? d.get("title").getAsString() : "";
                    contextMap.put(doctorId, new DoctorContext(doctorId, doctorName, department, title));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract doctor contexts from availability JSON: {}", e.getMessage());
        }
        return contextMap;
    }

    private static List<Long> extractTopDoctorIds(String availabilityJson, int limit) {
        List<Long> ids = new ArrayList<>();
        if (availabilityJson == null || availabilityJson.isEmpty() || limit <= 0) {
            return ids;
        }
        try {
            JsonObject root = JsonParser.parseString(availabilityJson).getAsJsonObject();
            if (root.has("error")) {
                return ids;
            }
            JsonArray doctors = root.getAsJsonArray("doctors");
            if (doctors == null) {
                return ids;
            }
            for (JsonElement el : doctors) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject d = el.getAsJsonObject();
                if (d.has("doctorId") && !d.get("doctorId").isJsonNull()) {
                    ids.add(d.get("doctorId").getAsLong());
                    if (ids.size() >= limit) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract doctorId from availability JSON: {}", e.getMessage());
        }
        return ids;
    }

    private static String normalizeScheduleDayForDoctorDb(String scheduleDay) {
        if (scheduleDay == null) {
            return "";
        }
        String s = scheduleDay.trim();
        if (s.matches("\\d{8}")) {
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8);
        }
        return s;
    }

    private static String extractJson(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            int endFence = s.lastIndexOf("```");
            if (endFence >= 0) {
                s = s.substring(0, endFence);
            }
            s = s.trim();
        }
        int jsonStart = s.indexOf('{');
        if (jsonStart > 0) {
            s = s.substring(jsonStart);
        }
        int lastBrace = s.lastIndexOf('}');
        if (lastBrace >= 0 && lastBrace < s.length() - 1) {
            s = s.substring(0, lastBrace + 1);
        }
        return s.trim();
    }
}
