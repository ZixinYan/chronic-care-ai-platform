---
name: query-all-doctors
description: 查询系统中所有医生的信息（包括科室、职称、经验等）。当需要了解医生画像、匹配专业、或初始化排班推荐流程时使用。触发词：医生列表、所有医生、医生信息。
---

# 查询所有医生信息

## 指令

1. **调用工具**：使用 **`queryAllDoctors`**（无需参数）。
2. **分析返回结果**：解析医生列表，关注以下字段：
   - `doctorId`：医生用户 ID（必填，用于最终推荐）
   - `doctorName`：医生姓名
   - `department`：科室，用于匹配业务需求
   - `title`：职称（主任医师、副主任医师、主治医师等）
   - `experience`：工作经验年限
   - `education`：学历
   - `bio`：简介
3. **筛选候选医生**：根据业务需求匹配科室和专业。

## 工具参数

无需参数。

## 返回 JSON 结构

```json
{
  "doctors": [
    {
      "doctorId": 1001,
      "doctorName": "张医生",
      "department": "内科",
      "title": "主任医师",
      "experience": 15,
      "education": "博士",
      "bio": "擅长心血管疾病诊治"
    },
    {
      "doctorId": 1002,
      "doctorName": "李医生",
      "department": "外科",
      "title": "副主任医师",
      "experience": 10,
      "education": "硕士",
      "bio": "擅长微创手术"
    }
  ]
}
```
