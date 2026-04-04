package com.zixin.aicapabilityprovider.prompt;

import com.zixin.aicapabilityapi.dto.GenerateScheduleRequest;
import com.zixin.aicapabilityprovider.constants.ScheduleConstants;

import java.util.List;
import java.util.Map;

public class SchedulePromptBuilder {

    private SchedulePromptBuilder() {}

    public static String buildUserPrompt(
            GenerateScheduleRequest request,
            String toolScheduleDay,
            String allDoctorsObservation,
            String leavesObservation,
            String availabilityObservation,
            List<String> detailObservations,
            Map<Long, DoctorContext> doctorContextMap) {

        StringBuilder sb = new StringBuilder();

        appendTaskSection(sb, request, toolScheduleDay);
        appendObservationsSection(sb, allDoctorsObservation, leavesObservation, availabilityObservation, detailObservations);
        appendOutputSchemaSection(sb, toolScheduleDay, doctorContextMap);
        appendRulesSection(sb);

        return sb.toString();
    }

    private static void appendTaskSection(StringBuilder sb, GenerateScheduleRequest request, String toolScheduleDay) {
        sb.append("# 排班任务\n\n");
        sb.append("**预约日期**: ").append(request.getScheduleDay()).append("\n");
        sb.append("**工具用 scheduleDay（YYYY-MM-DD）**: ").append(toolScheduleDay).append("\n");

        if (request.getBusinessRequirement() != null && !request.getBusinessRequirement().isEmpty()) {
            sb.append("**业务需求**: ").append(request.getBusinessRequirement()).append("\n");
        }
        if (Boolean.TRUE.equals(request.getSpecifyDoctor())) {
            sb.append("**是否指定医生**: 是\n");
            if (request.getDoctorId() != null) {
                sb.append("**指定医生用户 ID**: ").append(request.getDoctorId()).append("\n");
            }
        } else {
            sb.append("**是否指定医生**: 否\n");
        }
    }

    private static void appendObservationsSection(
            StringBuilder sb,
            String allDoctorsObservation,
            String leavesObservation,
            String availabilityObservation,
            List<String> detailObservations) {

        sb.append("\n# 观察数据 (Observations)\n\n");

        sb.append("## 1. 所有医生信息 (queryAllDoctors)\n");
        sb.append("以下是系统中所有医生的基本信息，包括科室、职称、工作经验等：\n");
        sb.append("```json\n").append(allDoctorsObservation).append("\n```\n");

        sb.append("\n## 2. 医生请假情况 (queryDoctorLeaves)\n");
        sb.append("以下是目标日期附近的已批准请假记录，排班时需排除这些医生的空闲时段：\n");
        sb.append("```json\n").append(leavesObservation).append("\n```\n");

        sb.append("\n## 3. 医生排班情况 (queryDoctorAvailabilityForDay)\n");
        sb.append("以下是目标日期各医生的排班情况，包含已有日程数量和状态：\n");
        sb.append("```json\n").append(availabilityObservation).append("\n```\n");

        int idx = 1;
        for (String detail : detailObservations) {
            sb.append("\n## 4.").append(idx++).append(" 候选医生详细排班 (queryDoctorScheduleDetailForDay)\n");
            sb.append("```json\n").append(detail).append("\n```\n");
        }
    }

    private static void appendOutputSchemaSection(StringBuilder sb, String toolScheduleDay, Map<Long, DoctorContext> doctorContextMap) {
        sb.append("\n# 输出要求\n\n");

        sb.append("## 输出格式\n");
        sb.append("请**直接输出**以下 JSON 格式（不要使用 Markdown 代码块包裹）：\n\n");

        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"schedules\": [\n");
        sb.append("    {\n");
        sb.append("      \"doctorId\": <从 Observation 中获取的医生ID，必填>,\n");
        sb.append("      \"doctorName\": \"<医生姓名，必填>\",\n");
        sb.append("      \"patientId\": <患者ID，如未知填 null>,\n");
        sb.append("      \"patientName\": \"<患者姓名，如未知填 null>\",\n");
        sb.append("      \"schedule\": \"<日程内容描述，必填，需具体明确>\",\n");
        sb.append("      \"scheduleCategory\": <日程类别ID，必填，见下方枚举>,\n");
        sb.append("      \"scheduleCategoryName\": \"<日程类别名称，必填>\",\n");
        sb.append("      \"scheduleDay\": \"").append(toolScheduleDay).append("\",\n");
        sb.append("      \"priority\": <优先级，必填，见下方枚举>,\n");
        sb.append("      \"priorityDesc\": \"<优先级描述，必填>\",\n");
        sb.append("      \"status\": \"<状态，必填，见下方枚举>\",\n");
        sb.append("      \"statusDesc\": \"<状态描述，必填>\",\n");
        sb.append("      \"startTime\": <开始时间戳(毫秒)，建议填入>,\n");
        sb.append("      \"endTime\": <结束时间戳(毫秒)，建议填入>,\n");
        sb.append("      \"result\": \"<执行结果，新日程填 null>\",\n");
        sb.append("      \"link\": \"<关联链接，如无填 null>\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"recommendation\": \"<推荐理由，必填，说明选择该医生的原因>\"\n");
        sb.append("}\n");
        sb.append("```\n\n");

        sb.append("## 枚举值定义\n\n");

        sb.append("**status 状态枚举**（必须使用以下值之一）：\n");
        sb.append("- ").append(ScheduleConstants.Status.getAllOptions()).append("\n\n");

        sb.append("**priority 优先级枚举**（必须使用以下值之一）：\n");
        sb.append("- ").append(ScheduleConstants.Priority.getAllOptions()).append("\n\n");

        sb.append("**scheduleCategory 日程类别枚举**（必须使用以下值之一）：\n");
        sb.append("- ").append(ScheduleConstants.ScheduleCategory.getAllOptions()).append("\n\n");

        if (doctorContextMap != null && !doctorContextMap.isEmpty()) {
            sb.append("## 可用医生信息参考\n");
            sb.append("以下是可供选择的医生信息，请确保 doctorId 和 doctorName 与之一致：\n");
            sb.append("```json\n").append("[\n");
            boolean first = true;
            for (Map.Entry<Long, DoctorContext> entry : doctorContextMap.entrySet()) {
                if (!first) sb.append(",\n");
                DoctorContext ctx = entry.getValue();
                sb.append("  {\"doctorId\": ").append(entry.getKey())
                  .append(", \"doctorName\": \"").append(ctx.getDoctorName()).append("\"")
                  .append(", \"department\": \"").append(ctx.getDepartment()).append("\"")
                  .append(", \"title\": \"").append(ctx.getTitle()).append("\"}");
                first = false;
            }
            sb.append("\n]\n```\n\n");
        }
    }

    private static void appendRulesSection(StringBuilder sb) {
        sb.append("# 排班规则\n\n");
        sb.append("1. **医生匹配优先级**：\n");
        sb.append("   - 优先根据业务需求匹配医生专业（科室、职称、经验）\n");
        sb.append("   - 如指定了医生ID，必须优先推荐该医生\n\n");

        sb.append("2. **排除规则**：\n");
        sb.append("   - 排除请假期间的医生（检查 leaves 列表中是否有 doctorId 对应且日期重叠）\n");
        sb.append("   - 如果某医生当天已有大量日程，考虑推荐其他医生\n\n");

        sb.append("3. **字段填充规则**：\n");
        sb.append("   - `doctorId` 和 `doctorName` 必须与 Observation 中的医生信息完全一致\n");
        sb.append("   - `scheduleDay` 必须使用工具用的日期格式 YYYY-MM-DD\n");
        sb.append("   - `schedule` 内容需具体明确，例如\"慢病随访-血压监测\"而非简单的\"随访\"\n");
        sb.append("   - `scheduleCategory` 和 `scheduleCategoryName` 必须成对出现且匹配\n");
        sb.append("   - `priority` 和 `priorityDesc` 必须成对出现且匹配\n");
        sb.append("   - `status` 和 `statusDesc` 必须成对出现且匹配\n");
        sb.append("   - 新创建的日程 `status` 应为 `PENDING`，`result` 应为 null\n\n");

        sb.append("4. **输出要求**：\n");
        sb.append("   - 必须输出至少一条推荐日程安排\n");
        sb.append("   - 不要使用 Markdown 代码块包裹最终 JSON\n");
        sb.append("   - 确保 JSON 格式正确，可直接解析\n");
    }

    public static class DoctorContext {
        private final Long doctorId;
        private final String doctorName;
        private final String department;
        private final String title;

        public DoctorContext(Long doctorId, String doctorName, String department, String title) {
            this.doctorId = doctorId;
            this.doctorName = doctorName;
            this.department = department;
            this.title = title;
        }

        public Long getDoctorId() { return doctorId; }
        public String getDoctorName() { return doctorName; }
        public String getDepartment() { return department; }
        public String getTitle() { return title; }
    }
}
