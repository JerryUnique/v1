# TODO List - 任务调度器功能扩展

## 1. 任务持久化与恢复执行
- [ ] 添加数据库依赖（H2内存数据库/MySQL）
- [ ] 创建任务实体类（Task Entity）
- [ ] 创建任务状态枚举（TaskStatus）
- [ ] 创建任务仓储层（TaskRepository）
- [ ] 扩展TaskService支持任务持久化
- [ ] 实现任务恢复机制（应用启动时）
- [ ] 添加任务状态更新逻辑
- [ ] 实现失败重试策略
- [ ] 编写单元测试

## 2. Cron 表达式支持（周期性任务）
- [ ] 添加Quartz依赖
- [ ] 扩展TaskRequest支持cron表达式
- [ ] 创建Cron任务处理类
- [ ] 集成Quartz调度器
- [ ] 提供cron任务管理接口（启停/修改）
- [ ] 编写单元测试

## 3. 任务执行日志记录与查询
- [ ] 创建任务执行日志实体类（TaskExecutionLog）
- [ ] 创建日志仓储层（TaskExecutionLogRepository）
- [ ] 扩展TaskService支持日志记录
- [ ] 提供日志查询接口（分页、筛选）
- [ ] 编写单元测试

## 实施顺序
1. 先实现基础的任务持久化和状态管理
2. 再添加Cron表达式支持
3. 最后实现日志记录和查询功能