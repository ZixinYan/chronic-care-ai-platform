---
name: query-doctor-leaves
description: 查询指定日期范围内的医生请假情况（已批准的请假）。当需要排除请假医生、确保排班医生可用时使用。触发词：请假、休假、医生请假。
---

# 查询医生请假情况

## 指令

1. **确定日期范围**：通常查询目标日期前后7天的请假记录，确保覆盖可能的请假影响。
2. **调用工具**：使用 **`queryDoctorLeaves`**（参数 startDay, endDay，日期格式为 YYYY-MM-DD）。
3. **分析返回结果**：解析请假列表，关注以下字段：
   - `doctorId`：请假医生的 ID
   - `doctorName`：医生姓名
   - `leaveType`：请假类型
   - `leaveTypeDesc`：请假类型描述
   - `startDay`：请假开始日期
   - `endDay`：请假结束日期
   - `reason`：请假原因
4. **排除请假医生**：在排班推荐时，排除目标日期在请假期间内的医生。

## 工具参数

| 参数名 | 类型 | 必填 | 描述 |
|-------|------|------|------|
| startDay | string | 是 | 查询开始日期，YYYY-MM-DD |
| endDay | string | 是 | 查询结束日期，YYYY-MM-DD |

## 返回 JSON 结构

```json
{
  "leaves": [
    {
      "doctorId": 1001,
      "doctorName": "张医生",
      "leaveType": "ANNUAL",
      "leaveTypeDesc": "年假",
      "startDay": "2024-01-10",
      "endDay": "2024-01-15",
      "reason": "家庭事务"
    },
    {
      "doctorId": 1002,
      "doctorName": "李医生",
      "leaveType": "SICK",
      "leaveTypeDesc": "病假",
      "startDay": "2024-01-12",
      "endDay": "2024-01-14",
      "reason": "身体不适"
    }
  ]
}
```

## 请假类型

| 类型 | 描述 |
|------|------|
| ANNUAL | 年假 |
| SICK | 病假 |
| PERSONAL | 事假 |
| MATERNITY | 产假 |
| PATERNITY | 陪产假 |
| MARRIAGE | 婚假 |
