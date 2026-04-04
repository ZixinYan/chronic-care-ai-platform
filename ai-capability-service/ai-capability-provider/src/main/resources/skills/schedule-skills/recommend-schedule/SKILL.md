---
name: recommend-schedule
description: 在已通过工具拿到真实医生与排班数据后，生成最终 JSON 推荐。健康报告等业务要求 schedules 中必须含 doctorId（与工具返回一致）。触发词：最终推荐、输出 JSON、排班结果。
---

# 生成排班推荐

## 指令

1. **确认数据完整性**：必须先调用 `queryAllDoctors`、`queryDoctorLeaves`、`queryDoctorAvailabilityForDay`（及必要时 `queryDoctorScheduleDetailForDay`）拿到真实数据。
2. **应用决策原则**：
   - 科室匹配：优先选择与业务需求科室匹配的医生
   - 职称权重：主任医师 > 副主任医师 > 主治医师
   - 负载均衡：优先当日负载适中（约 4-6 待处理）的医生
   - 排除请假：检查请假记录，排除目标日期在请假期间的医生
   - 时段选择：优先 PENDING 状态的时段
3. **输出**：仅输出**纯 JSON**，无 Markdown 代码块，无任何说明文字。
4. **必填字段**：每条 `schedules[]` 必须含 **`doctorId`**（Long，等于工具 JSON 中的 `doctors[].doctorId`），禁止编造。

## 输出 JSON 规范

输出 JSON 必须与 `ScheduleVO` 字段完全一致：

```json
{
  "schedules": [
    {
      "id": null,
      "doctorId": 1001,
      "doctorName": "张医生",
      "patientId": null,
      "patientName": null,
      "schedule": "健康报告审核",
      "scheduleCategory": 2,
      "scheduleCategoryName": "审核",
      "scheduleDay": "2024-01-15",
      "priority": 1,
      "priorityDesc": "高",
      "status": "PENDING",
      "statusDesc": "待处理",
      "result": null,
      "link": null,
      "startTime": 1705276800000,
      "endTime": 1705280400000
    }
  ],
  "recommendation": "推荐理由（中文，具体说明科室/职称/负载/时段）"
}
```

## 字段说明

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| id | Long | 否 | 日程ID，新建时填 null |
| doctorId | Long | 是 | 医生用户 ID，必须来自工具返回 |
| doctorName | String | 是 | 医生姓名 |
| patientId | Long | 否 | 患者ID |
| patientName | String | 否 | 患者姓名 |
| schedule | String | 是 | 日程内容描述 |
| scheduleCategory | Integer | 否 | 日程类别ID |
| scheduleCategoryName | String | 否 | 日程类别名称 |
| scheduleDay | String | 是 | 日程日期，YYYY-MM-DD |
| priority | Integer | 否 | 优先级（1-高，2-中，3-低） |
| priorityDesc | String | 否 | 优先级描述 |
| status | String | 是 | 状态（PENDING/IN_PROGRESS/COMPLETED） |
| statusDesc | String | 否 | 状态描述 |
| result | String | 否 | 执行结果 |
| link | String | 否 | 关联链接 |
| startTime | Long | 否 | 开始时间（Unix 毫秒） |
| endTime | Long | 否 | 结束时间（Unix 毫秒） |

## 质量检查

- [ ] `doctorId` 来自工具返回值
- [ ] `scheduleDay` 为 YYYY-MM-DD
- [ ] `startTime`/`endTime` 为 Unix 毫秒 Long 或 null
- [ ] JSON 可被严格解析
- [ ] 至少返回一条推荐日程
