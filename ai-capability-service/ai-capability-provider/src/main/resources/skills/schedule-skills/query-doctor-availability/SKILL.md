---
name: query-doctor-availability
description: 查询指定日期所有有排班记录的医生出勤情况与画像信息。当需要获取某日所有候选医生概览、评估医生负载、或健康报告上传需分配审核医生时使用。触发词：排班、医生列表、出勤、负载。
---

# 查询医生出勤情况

## 指令

当需要获取医生排班概览时，请执行以下操作：

1. **确认日期参数**：用户可能给出 `yyyyMMdd` 或 `YYYY-MM-DD`；调用工具时请使用 `YYYY-MM-DD` 格式。
2. **确认业务需求**：提取 `businessRequirement`（可选），用于筛选匹配的医生。
3. **调用工具**：使用 **`queryDoctorAvailabilityForDay`**（参数 scheduleDay, businessRequirement）。
4. **分析返回结果**：解析医生列表，关注以下字段：
   - `doctorId`：医生用户 ID（必填，用于最终推荐）
   - `department`：科室，用于匹配业务需求
   - `title`：职称，主任医师优先级最高
   - `experience`：工作经验年限
   - `pendingSchedules` + `inProgressSchedules`：当日负载
5. **筛选候选医生**：根据科室匹配、职称、负载情况筛选出 2-3 名候选医生。

## 工具参数

| 参数名 | 类型 | 必填 | 描述 |
|-------|------|------|------|
| scheduleDay | string | 是 | YYYY-MM-DD |
| businessRequirement | string | 否 | 业务需求描述 |

## 返回 JSON 结构

```json
{
  "scheduleDay": "2024-01-15",
  "businessRequirement": "健康报告审核",
  "doctors": [
    {
      "doctorId": 1001,
      "doctorName": "张医生",
      "department": "内科",
      "title": "主任医师",
      "experience": 15,
      "totalSchedules": 8,
      "pendingSchedules": 4,
      "inProgressSchedules": 2
    }
  ]
}
```
