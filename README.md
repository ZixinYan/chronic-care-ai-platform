
#  chromic-care-ai-platform

**AI-driven Chronic Disease Management & Medical Assistance Platform**

## 📌 项目简介

**chromic-care-ai-platform** 是一个面向中老年慢性病（以糖尿病为核心）的 **智慧医疗诊疗与健康管理平台**。
系统以患者健康数据为核心，结合 AI 预测模型、RAG 医学知识库和多角色协同机制，为 **患者、家属、医生/疗养师及管理人员** 提供持续、可解释、可审计的智能辅助能力。

本项目为与东软集团项目导向毕设课题

---

## 🎯 设计目标

* 以 **健康数据驱动** 为中心，为患者提供智能化的健康管理服务
* 支持 **预测、预警、解释、追溯**
* 多角色协同（患者 / 家属 / 医生 / 管理员）
* AI 参与但 **不直接替代医疗决策**
* 符合微服务、领域拆分、权限隔离的工程实践

---
## 🛡 技术栈（Tech Stack）
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk\&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot\&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.x-6DB33F?logo=spring\&logoColor=white)
![Apache Dubbo](https://img.shields.io/badge/Apache%20Dubbo-3.x-007AFF?logo=apache\&logoColor=white)
![Nacos](https://img.shields.io/badge/Nacos-3.x-2AC7F4)
![Gateway](https://img.shields.io/badge/API%20Gateway-Spring%20Cloud%20Gateway-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-Role%20Based-6DB33F?logo=springsecurity\&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-black?logo=jsonwebtokens)
![SSO](https://img.shields.io/badge/SSO-Single%20Sign%20On-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql\&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis\&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Message%20Queue-231F20?logo=apachekafka\&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Search-005571?logo=elasticsearch\&logoColor=white)
![Python](https://img.shields.io/badge/Python-AI%20Service-3776AB?logo=python\&logoColor=white)
![LLM](https://img.shields.io/badge/LLM-RAG%20Enhanced-purple)
![Spring AI](https://img.shields.io/badge/Spring%20AI-Alibaba-00B4A2)

---
## 👥 角色与使用场景

### 1️⃣ 用户层（患者 & 家属）

#### 核心功能

* **日常健康数据采集**

    * 血糖（核心）
    * 可扩展：血压、心率、体重等
* **数据来源**

    * 用户手动录入
    * 第三方检测机构报告导入
    * HIS 系统对接（预留）

#### 智能能力

* **血糖趋势预测**

    * 短期 / 中期预测
    * 模型输出结构化结果
* **异常预警与风险提示**

    * 超阈值告警
    * 趋势性风险提示
* **AI 健康建议**

    * 标注为「未经过医生确认」
    * 高风险场景自动报警

#### 家属联动

* 患者与家属绑定
* 异常情况通知：

    * 站内信
    * 短信
* 告警阈值可配置（后续扩展）

#### 报告与画像

* **医学标准化报告**

    * 指标说明
    * 风险区间
    * 建议界限
* **用户健康画像**

    * 长期指标趋势
    * 行为与健康特征建模

---

### 2️⃣ 医生 / 疗养师侧

#### 数据与辅助

* 患者健康数据与分析报告查看
* AI 预测结果与异常记录
* RAG 医学知识辅助参考

#### 工作支持

* **患者报告自动汇总**
* **工作日程管理**

    * 周期性患者情况总结
    * AI 辅助分析重点风险人群

#### 扩展场景（规划中）

* 吃 / 穿 / 住 / 行 / 医 / 养 / 康 / 旅 的健康关联分析
* 医养结合建议输出

---

### 3️⃣ 管理侧（平台管理）

* **SSO 登录**
* **角色分权**

    * 患者
    * 家属
    * 医生 / 疗养师
    * 管理员
* 系统配置管理
* 审计与日志追踪（规划）

---

## 🧠 AI 能力说明

### 预测模型

* 输入：历史健康数据
* 输出：
    * 预测值
    * 置信区间
    * 风险等级
* 预测结果 **仅作为辅助参考**，通过医生确认后发送患者

### RAG 知识库

* 医学常识
* 慢性病管理规范
* 健康生活方式建议
* 用于：

    * AI 问答
    * 报告解释
    * 医生辅助参考

---

## 🧱 系统架构（概览）

```text
                ┌─────────────┐
                │   前端 Web   │
                └──────┬──────┘
                ┌——————────────┐
                │Nginx/Sentinel│
                └──——──┬─——────┘
        ┌──────────────┼──────────────┐
        │              │              │
  用户服务        医疗分析服务     AI 服务
 (账号/角色)     (报告/预测)   (模型/RAG)   · · ·
        │              │              │
        └──────┬───────┴───────┬──────┘
               │               │
           消息队列          数据存储
        (Kafka/RabbitMQ)   (MySQL / ES)
```

---

## 🛠 技术栈选型

### 后端
* Spring Boot / Spring Cloud
* SpringAI
* Apache Dubbo
* Spring Security
* JWT / 双 Token 机制

### 数据
* MySQL
* Redis
* Elasticsearch
* RocketMQ / Kafka

### AI
* Python（模型服务）
* RAG（PineCone/PgSQL + DeepSeek）

---

## ⚠️ 声明

* 本项目为 **项目原型系统**
* 所有 AI 输出 **不构成医疗诊断意见**
* 实际医疗应用需符合相关法律法规与医疗规范
