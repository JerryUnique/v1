package com.example.scheduler.service;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.repository.TaskExecutionLogRepository;
import com.example.scheduler.repository.TaskRepository;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 增强的任务服务类
 * 提供任务持久化、Cron表达式支持和日志记录功能
 */
@Service
@Transactional
public class EnhancedTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTaskService.class);
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private TaskExecutionLogRepository taskExecutionLogRepository;
    
    @Autowired
    private Scheduler quartzScheduler;
    
    private TaskScheduler taskScheduler;
    
    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(10);
        threadPoolTaskScheduler.setThreadNamePrefix("enhanced-task-");
        threadPoolTaskScheduler.initialize();
        this.taskScheduler = threadPoolTaskScheduler;
        
        // 应用启动时恢复任务
        recoverTasksOnStartup();
    }
    
    // ============ 功能1：任务持久化与恢复执行 ============
    
    /**
     * 调度任务并持久化
     */
    public Long scheduleTaskWithPersistence(TaskRequest request) {
        logger.info("Scheduling task with persistence: {}", request.getTaskName());
        
        // 创建任务实体
        Task task = new Task();
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        
        // 支持两种时间格式（向后兼容）
        if (request.getScheduledTime() != null) {
            task.setScheduledTime(request.getScheduledTime());
        } else if (request.getRunAt() != null) {
            task.setScheduledTime(LocalDateTime.ofInstant(request.getRunAt(), ZoneId.systemDefault()));
        }
        
        // 保存到数据库
        Task savedTask = taskRepository.save(task);
        
        // 调度任务执行
        scheduleTaskExecution(savedTask);
        
        return savedTask.getId();
    }
    
    /**
     * 应用启动时恢复待执行任务
     */
    public void recoverTasksOnStartup() {
        logger.info("Recovering pending tasks on startup");
        
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);
        List<Task> pendingScheduledTasks = taskRepository.findPendingTasksByScheduledTime(LocalDateTime.now().plusYears(10));
        
        for (Task task : pendingTasks) {
            if (task.getScheduledTime() != null && task.getScheduledTime().isAfter(LocalDateTime.now())) {
                scheduleTaskExecution(task);
            }
        }
        
        // 恢复Cron任务
        List<Task> cronTasks = taskRepository.findPendingCronTasks();
        for (Task task : cronTasks) {
            try {
                scheduleCronTaskExecution(task);
            } catch (Exception e) {
                logger.error("Failed to recover cron task: {}", task.getTaskName(), e);
            }
        }
        
        logger.info("Recovered {} scheduled tasks and {} cron tasks", 
                   pendingScheduledTasks.size(), cronTasks.size());
    }
    
    /**
     * 重试失败的任务
     */
    public void retryTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        if (task.getRetryCount() >= task.getMaxRetries()) {
            throw new IllegalStateException("Task has exceeded maximum retry attempts");
        }
        
        task.setRetryCount(task.getRetryCount() + 1);
        task.setStatus(TaskStatus.PENDING);
        task.setErrorMessage(null);
        
        taskRepository.save(task);
        
        // 重新调度
        if (task.getCronExpression() != null) {
            try {
                scheduleCronTaskExecution(task);
            } catch (Exception e) {
                logger.error("Failed to reschedule cron task for retry", e);
            }
        } else {
            // 对于一次性任务，延迟1分钟后重试
            task.setScheduledTime(LocalDateTime.now().plusMinutes(1));
            taskRepository.save(task);
            scheduleTaskExecution(task);
        }
        
        logger.info("Task {} scheduled for retry (attempt {}/{})", 
                   task.getTaskName(), task.getRetryCount(), task.getMaxRetries());
    }
    
    // ============ 功能2：Cron表达式支持 ============
    
    /**
     * 调度Cron任务
     */
    public Long scheduleCronTask(TaskRequest request) {
        if (request.getCronExpression() == null || request.getCronExpression().trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression is required");
        }
        
        // 验证Cron表达式
        try {
            new CronExpression(request.getCronExpression());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + request.getCronExpression());
        }
        
        logger.info("Scheduling cron task: {} with expression: {}", 
                   request.getTaskName(), request.getCronExpression());
        
        // 创建任务实体
        Task task = new Task();
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setCronExpression(request.getCronExpression());
        task.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        
        // 保存到数据库
        Task savedTask = taskRepository.save(task);
        
        // 使用Quartz调度Cron任务
        try {
            scheduleCronTaskExecution(savedTask);
        } catch (Exception e) {
            logger.error("Failed to schedule cron task", e);
            throw new RuntimeException("Failed to schedule cron task", e);
        }
        
        return savedTask.getId();
    }
    
    /**
     * 停止Cron任务
     */
    public void stopCronTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setStatus(TaskStatus.CANCELLED);
        taskRepository.save(task);
        
        // 从Quartz中移除任务
        try {
            JobKey jobKey = JobKey.jobKey("task-" + taskId, "default");
            quartzScheduler.deleteJob(jobKey);
            logger.info("Stopped cron task: {}", task.getTaskName());
        } catch (SchedulerException e) {
            logger.error("Failed to stop cron task from Quartz", e);
        }
    }
    
    // ============ 功能3：任务执行日志记录与查询 ============
    
    /**
     * 记录任务执行日志
     */
    public void logTaskExecution(Long taskId, String taskName, TaskStatus status, 
                                String errorMessage, Long durationMs) {
        TaskExecutionLog log = new TaskExecutionLog();
        log.setTaskId(taskId);
        log.setTaskName(taskName);
        log.setExecutionTime(LocalDateTime.now());
        log.setExecutionResult(status);
        log.setErrorMessage(errorMessage);
        log.setExecutionDurationMs(durationMs);
        
        taskExecutionLogRepository.save(log);
        logger.debug("Logged task execution: {} - {}", taskName, status);
    }
    
    /**
     * 获取任务执行日志
     */
    public List<TaskExecutionLog> getTaskExecutionLogs(Long taskId) {
        return taskExecutionLogRepository.findByTaskIdOrderByExecutionTimeDesc(taskId);
    }
    
    /**
     * 分页获取执行日志
     */
    public Page<TaskExecutionLog> getExecutionLogsWithPagination(Pageable pageable) {
        return taskExecutionLogRepository.findAll(pageable);
    }
    
    /**
     * 根据状态获取执行日志
     */
    public Page<TaskExecutionLog> getExecutionLogsByStatus(TaskStatus status, Pageable pageable) {
        return taskExecutionLogRepository.findByExecutionResult(status, pageable);
    }
    
    /**
     * 根据时间范围获取执行日志
     */
    public Page<TaskExecutionLog> getExecutionLogsByTimeRange(LocalDateTime startTime, 
                                                            LocalDateTime endTime, 
                                                            Pageable pageable) {
        return taskExecutionLogRepository.findByExecutionTimeBetween(startTime, endTime, pageable);
    }
    
    /**
     * 根据任务名称搜索执行日志
     */
    public Page<TaskExecutionLog> searchExecutionLogsByTaskName(String taskName, Pageable pageable) {
        return taskExecutionLogRepository.findByTaskNameContainingIgnoreCaseOrderByExecutionTimeDesc(taskName, pageable);
    }
    
    // ============ 私有辅助方法 ============
    
    /**
     * 调度任务执行（一次性任务）
     */
    private void scheduleTaskExecution(Task task) {
        if (task.getScheduledTime() == null || task.getScheduledTime().isBefore(LocalDateTime.now())) {
            return;
        }
        
        Instant scheduledInstant = task.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant();
        
        taskScheduler.schedule(() -> executeTask(task), scheduledInstant);
        logger.debug("Scheduled task {} for execution at {}", task.getTaskName(), task.getScheduledTime());
    }
    
    /**
     * 调度Cron任务执行
     */
    private void scheduleCronTaskExecution(Task task) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(SpringTaskExecutionJob.class)
                .withIdentity("task-" + task.getId(), "default")
                .usingJobData("taskId", task.getId())
                .build();
        
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger-" + task.getId(), "default")
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();
        
        quartzScheduler.scheduleJob(jobDetail, trigger);
        logger.debug("Scheduled cron task {} with expression {}", task.getTaskName(), task.getCronExpression());
    }
    
    /**
     * 执行任务
     */
    public void executeTask(Task task) {
        long startTime = System.currentTimeMillis();
        TaskStatus executionResult = TaskStatus.COMPLETED;
        String errorMessage = null;
        
        try {
            // 更新任务状态为运行中
            task.setStatus(TaskStatus.RUNNING);
            taskRepository.save(task);
            
            logger.info("Executing task: {}", task.getTaskName());
            
            // 模拟任务执行（实际应用中这里会调用具体的任务逻辑）
            Thread.sleep(1000); // 模拟任务执行时间
            
            // 任务执行成功
            task.setStatus(TaskStatus.COMPLETED);
            logger.info("Task completed successfully: {}", task.getTaskName());
            
        } catch (Exception e) {
            // 任务执行失败
            executionResult = TaskStatus.FAILED;
            errorMessage = e.getMessage();
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            
            logger.error("Task execution failed: {}", task.getTaskName(), e);
        } finally {
            long executionDuration = System.currentTimeMillis() - startTime;
            taskRepository.save(task);
            
            // 记录执行日志
            logTaskExecution(task.getId(), task.getTaskName(), executionResult, errorMessage, executionDuration);
            
            // 如果任务失败且未超过最大重试次数，尝试重试
            if (executionResult == TaskStatus.FAILED && task.getRetryCount() < task.getMaxRetries()) {
                try {
                    retryTask(task.getId());
                } catch (Exception retryException) {
                    logger.error("Failed to schedule retry for task: {}", task.getTaskName(), retryException);
                }
            }
        }
    }
    
    // ============ Quartz Job类 ============
    
    /**
     * Quartz任务执行类
     */
    public static class SpringTaskExecutionJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            Long taskId = dataMap.getLong("taskId");
            
            // 这里需要通过ApplicationContext获取EnhancedTaskService实例
            // 由于这是静态类，我们需要通过其他方式获取Spring Bean
            logger.info("Executing cron job for task ID: {}", taskId);
        }
    }
}