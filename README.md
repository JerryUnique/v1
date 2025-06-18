# v1
# 🎯 Task Scheduler Platform (Spring Boot)

这是一个基于 **Java 8 + Spring Boot** 实现的轻量级任务调度平台。你可以通过 REST 接口注册任务，并设置在指定时间点执行。适合作为后端服务调度中心的基础框架。

---

## 🚀 项目亮点

- 使用 `ThreadPoolTaskScheduler` 进行任务调度
- 接口化任务注册，支持任意时间触发
- 简洁易用的 JSON 参数接口
- 可扩展为持久化任务调度中心（配合 Quartz）

---

## 📦 技术栈

| 技术 | 说明 |
|------|------|
| Java | 1.8 |
| Spring Boot | 2.3.0 |
| Maven | 3.x |
| REST API | 任务注册与触发 |
| JUnit 5 | 单元测试 |

---

## 🏗️ 工程结构
task-scheduler/
├── src/
│ ├── main/
│ │ ├── java/com/example/scheduler/
│ │ │ ├── controller/TaskController.java
│ │ │ ├── service/TaskService.java
│ │ │ ├── config/SchedulerConfig.java
│ │ │ └── model/TaskRequest.java
│ │ └── resources/application.yml
├── test/
│ └── java/com/example/scheduler/TaskServiceTest.java
├── pom.xml
└── run.sh
