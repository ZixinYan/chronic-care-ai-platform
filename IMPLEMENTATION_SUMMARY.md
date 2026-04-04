# 血糖预测与AI报告生成功能实现总结

## 一、功能概述

本次实现新增了以下两个核心功能模块：

### 1. 患者历史健康报告查询
- ✅ 获取近5次报告接口
- ✅ 获取近10天AI总结信息接口

### 2. 血糖数据预测与预警
- ✅ 血糖数据预测接口（支持多维度数据）
- ✅ AI报告自动生成并入库
- ✅ 血糖阈值检测与预警短信发送

---

## 二、血糖阈值规则

| 用餐类型 | 阈值 (mmol/L) |
|---------|--------------|
| 空腹 (mealType=1) | > 8.3 |
| 餐后1h (mealType=2) | > 12.7 |
| 餐后2h (mealType=3) | > 11.1 |
| 餐后3h (mealType=4) | > 10.0 |

> 单位转换：1 mg/dL = 0.0555 mmol/L

---

## 三、新增接口清单

### 3.1 HealthReportController（健康报告中心）

| 接口 | 方法 | 说明 |
|-----|------|------|
| GET /health/report/recent | getRecentReports | 获取最近N次报告 |
| GET /health/report/ai-summary | getRecentAISummary | 获取最近N天AI总结 |

### 3.2 GlucosePredictionController（血糖预测服务）

| 接口 | 方法 | 说明 |
|-----|------|------|
| POST /glucose/predict | predictGlucose | 血糖预测（完整版） |
| POST /glucose/predict/simple | predictGlucoseSimple | 血糖预测（简化版） |

### 3.3 GlucoseReportController（血糖报告流程）

| 接口 | 方法 | 说明 |
|-----|------|------|
| POST /glucose-report/predict-and-generate | predictAndGenerateReport | 完整预测+报告生成流程 |
| POST /glucose-report/check-alert | checkGlucoseAlert | 检查阈值并发送预警 |

---

## 四、新增Dubbo API接口

### 4.1 HealthReportAPI 扩展方法

```java
GetRecentReportsResponse getRecentReports(GetRecentReportsRequest request);
GetRecentAISummaryResponse getRecentAISummary(GetRecentAISummaryRequest request);
GenerateAIReportResponse generateAIReport(GenerateAIReportRequest request);
CheckGlucoseAlertResponse checkGlucoseAlert(CheckGlucoseAlertRequest request);
```

### 4.2 GlucosePredictionAPI（新增）

```java
PredictGlucoseResponse predictGlucose(PredictGlucoseRequest request);
```

---

## 五、数据模型说明

### 5.1 血糖数据维度

| 字段 | 全称 | 单位 | 来源 |
|-----|------|-----|------|
| cbg | Continuous Blood Glucose | mg/dL | 连续血糖监测仪(CGM) |
| finger | Fingerstick Blood Glucose | mg/dL | 传统血糖仪 |
| basal | Basal Rate | U/h | 胰岛素泵 |
| hr | Heart Rate | bpm | 可穿戴设备 |
| gsr | Galvanic Skin Response | - | 可穿戴设备 |
| carbInput | Carbohydrate Input | g | 患者记录 |
| bolus | Bolus | U | 胰岛素泵 |

### 5.2 新增数据库表

1. **care_platform_ai_summary** - AI健康总结表
2. **care_platform_glucose_data** - 血糖数据表
3. **care_platform_glucose_prediction** - 血糖预测记录表

SQL文件位置：`sql/glucose_prediction.sql`

---

## 六、业务流程

### 6.1 完整预测流程（v2.1 更新）

```
用户上传血糖数据
        ↓
POST /glucose-report/predict-and-generate
        ↓
调用 GlucosePredictionAPI.predictGlucose
        ↓
【TODO: 调用外部Python预测接口】
        ↓
获取未来血糖预测值（离散数据）
        ↓
调用 HealthReportAPI.generateAIReport
        ↓
┌─────────────────────────────────────────┐
│           综合分析阶段                    │
│  1. 分析当前血糖情况（平均值/最高/最低）   │
│  2. 分析预测血糖情况                      │
│  3. 对比当前vs预测趋势                    │
│  4. 检测是否超过阈值                      │
│  5. 生成综合健康报告（Markdown格式）      │
└─────────────────────────────────────────┘
        ↓
调用 HealthReportAPI.uploadReport（复用现有审核流程）
        ↓
报告进入审核队列（医生可审核）
        ↓
检测CBG是否超过阈值？
    ┌───┴───┐
   否      是
    ↓       ↓
  完成   查询患者紧急联系人
            ↓
         解析emergencyPhone
            ↓
         调用 SMSAPI.sendSMS
            ↓
         发送预警短信给家属
```

---

## 七、待办事项（TODO）

### 7.1 Python预测服务集成
**位置**：`GlucosePredictionServiceImpl.predictWithPythonService()`

```java
// 【TODO: 调用外部Python预测接口】
// 当前使用简单线性预测作为示例
// 实际应该调用 glucose-ai-prediction 模块中的Python服务
```

### 7.2 短信模板配置
**位置**：`HealthReportServiceImpl.sendGlucoseAlertSMS()`

```java
SendSMSRequest smsRequest = new SendSMSRequest();
smsRequest.setPhone(emergencyPhone);
smsRequest.setCode(smsContent);
smsRequest.setTemplateId("GLUCOSE_ALERT"); // 【TODO: 替换为实际模板ID】
```

### 7.3 AI总结数据生成
**位置**：`HealthReportServiceImpl.getRecentAISummary()`

当前从数据库查询，需要配合定时任务或AI服务自动生成AI总结数据。

---

## 八、文件变更清单

### 8.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `health-center-api/dto/GetRecentReportsRequest.java` | 查询最近报告请求 |
| `health-center-api/dto/GetRecentReportsResponse.java` | 查询最近报告响应 |
| `health-center-api/dto/GetRecentAISummaryRequest.java` | 查询AI总结请求 |
| `health-center-api/dto/GetRecentAISummaryResponse.java` | 查询AI总结响应 |
| `health-center-api/dto/GenerateAIReportRequest.java` | 生成AI报告请求 |
| `health-center-api/dto/GenerateAIReportResponse.java` | 生成AI报告响应 |
| `health-center-api/dto/CheckGlucoseAlertRequest.java` | 检查阈值请求 |
| `health-center-api/dto/CheckGlucoseAlertResponse.java` | 检查阈值响应 |
| `health-center-api/dto/GlucoseDataDimensions.java` | 血糖数据维度 |
| `health-center-api/vo/AISummaryVO.java` | AI总结VO |
| `health-center-api/po/AISummary.java` | AI总结实体 |
| `health-center-provider/mapper/AISummaryMapper.java` | AI总结Mapper |
| `health-center-consumer/controller/GlucoseReportController.java` | 血糖报告流程控制器 |
| `blood-glucose-api/api/GlucosePredictionAPI.java` | 血糖预测API |
| `blood-glucose-api/dto/PredictGlucoseRequest.java` | 血糖预测请求 |
| `blood-glucose-api/dto/PredictGlucoseResponse.java` | 血糖预测响应 |
| `blood-glucose-consumer/controller/GlucosePredictionController.java` | 血糖预测控制器 |
| `blood-glucose-provider/service/GlucosePredictionServiceImpl.java` | 血糖预测服务实现 |
| `sql/glucose_prediction.sql` | 数据库DDL |

### 8.2 修改文件

| 文件路径 | 变更说明 |
|---------|---------|
| `health-center-api/api/HealthReportAPI.java` | 新增4个接口方法 |
| `health-center-api/enums/ReportType.java` | 新增AI_GENERATED类型 |
| `health-center-consumer/controller/HealthReportController.java` | 新增2个HTTP接口 |
| `health-center-provider/service/HealthReportServiceImpl.java` | 实现新增接口方法 |
| `API-DOC.md` | 新增接口文档 |

---

## 九、权限要求

所有新增接口都需要 **PATIENT** 角色：

```java
@RequireRole("PATIENT")
```

患者只能查看/操作自己的数据，系统通过 `UserInfoManager.getUserIdOrThrow()` 获取当前登录用户ID进行权限控制。

---

## 十、报告内容结构

生成的健康报告采用Markdown格式，包含以下章节：

```markdown
# 血糖监测与预测分析报告

## 一、当前血糖监测情况
### 1. 统计指标
- 平均值、最高值、最低值、监测点数
### 2. 原始监测数据
- 表格展示最近20个监测点

## 二、血糖预测结果
### 1. 预测统计
- 用餐类型、预测时长、预测平均值、最高值、最低值
### 2. 阈值检测
- 当前阈值、检测结果（正常/超标预警）
### 3. 预测数据明细
- 表格展示所有预测点及状态

## 三、综合分析与建议
### 1. 趋势分析
- 当前vs预测趋势对比（平稳/上升/下降）
### 2. 健康建议
- 正常维持建议 or 预警干预建议

## 四、监测数据维度
- CGM、指尖血、基础率、心率、皮肤电反应、碳水摄入、胰岛素数据量
```

报告通过 `uploadReport` 接口上传，复用现有审核流程，医生可以在工作台审核AI生成的报告。

## 十一、后续优化建议

1. **Python服务集成**：实现真实的LSTM/CNN预测模型调用
2. **缓存优化**：对频繁查询的最近报告和AI总结添加Redis缓存
3. **批量处理**：支持批量血糖数据上传和预测
4. **报告模板**：优化AI报告的展示格式和内容
5. **预警规则**：支持自定义血糖阈值配置
6. **数据分析**：增加血糖趋势分析和可视化图表

---

**实现完成时间**：2026-04-01  
**版本**：v2.0