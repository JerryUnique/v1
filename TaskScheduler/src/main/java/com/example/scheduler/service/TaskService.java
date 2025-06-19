package com.example.scheduler.service;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private ThreadPoolTaskScheduler scheduler;
    
    @Autowired
    private ConcurrencyControlService concurrencyControlService;

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(10); // 增加默认线程池大小
        threadPoolTaskScheduler.setThreadNamePrefix("task-scheduler-");
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setAwaitTerminationSeconds(60);
        threadPoolTaskScheduler.initialize();
        this.scheduler = threadPoolTaskScheduler;
        
        // 初始化并发控制配置
        initializeConcurrencyControl();
        
        logger.info("TaskService 初始化完成，线程池大小: {}", threadPoolTaskScheduler.getPoolSize());
    }

    /**
     * 初始化并发控制配置
     */
    private void initializeConcurrencyControl() {
        // 为不同类型的任务设置默认的并发控制
        concurrencyControlService.setMaxConcurrency(TaskType.DATA_PROCESSING, 3);
        concurrencyControlService.setMaxConcurrency(TaskType.REPORT_GENERATION, 2);
        concurrencyControlService.setMaxConcurrency(TaskType.FILE_UPLOAD, 5);
        concurrencyControlService.setMaxConcurrency(TaskType.EMAIL_SENDING, 2);
        concurrencyControlService.setMaxConcurrency(TaskType.DEFAULT, 1);
    }

    /**
     * 调度任务（带并发控制）
     */
    public ScheduledFuture<?> scheduleTask(TaskRequest request) {
        Instant runAt = request.getRunAt();
        
        return scheduler.schedule(() -> {
            // 使用并发控制执行任务
            concurrencyControlService.executeWithConcurrencyControl(request, () -> {
                executeTaskLogic(request);
            });
        }, runAt);
    }

    /**
     * 立即执行任务（带并发控制）
     */
    public void executeTaskImmediately(TaskRequest request) {
        concurrencyControlService.executeWithConcurrencyControl(request, () -> {
            executeTaskLogic(request);
        });
    }

    /**
     * 实际的任务执行逻辑
     */
    private void executeTaskLogic(TaskRequest request) {
        logger.info("执行任务: {}, 类型: {}", request.getTaskName(), request.getTaskType());
        
        try {
            // 模拟不同类型任务的执行时间
            long executionTime = getExecutionTimeByTaskType(request.getTaskType());
            Thread.sleep(executionTime);
            
            logger.info("任务 {} 执行完成，耗时: {}ms", request.getTaskName(), executionTime);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("任务 {} 执行被中断", request.getTaskName());
            throw new RuntimeException("任务执行被中断", e);
        } catch (Exception e) {
            logger.error("任务 {} 执行失败: {}", request.getTaskName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据任务类型获取模拟执行时间
     */
    private long getExecutionTimeByTaskType(TaskType taskType) {
        switch (taskType) {
            case DATA_PROCESSING:
                return 2000; // 2秒
            case REPORT_GENERATION:
                return 3000; // 3秒
            case FILE_UPLOAD:
                return 1000; // 1秒
            case EMAIL_SENDING:
                return 500; // 0.5秒
            default:
                return 1000; // 1秒
        }
    }

    /**
     * 获取当前线程池大小
     */
    public int getThreadPoolSize() {
        return scheduler.getPoolSize();
    }

    /**
     * 动态更新线程池大小
     */
    public void updateThreadPoolSize(int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException("线程池大小必须大于0");
        }
        
        int oldSize = scheduler.getPoolSize();
        scheduler.setPoolSize(newSize);
        
        logger.info("线程池大小已从 {} 更新为 {}", oldSize, newSize);
    }

    /**
     * 获取活跃线程数
     */
    public int getActiveThreadCount() {
        return scheduler.getActiveCount();
    }

    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        return String.format("线程池状态 - 池大小: %d, 活跃线程: %d", 
            scheduler.getPoolSize(), scheduler.getActiveCount());
    }

    /**
     * 设置任务类型的最大并发数
     */
    public void setTaskTypeConcurrency(TaskType taskType, int maxConcurrency) {
        concurrencyControlService.setMaxConcurrency(taskType, maxConcurrency);
        logger.info("任务类型 {} 的最大并发数已设置为 {}", taskType, maxConcurrency);
    }

    /**
     * 获取任务类型的运行状态
     */
    public String getTaskTypeStatus(TaskType taskType) {
        int running = concurrencyControlService.getRunningTaskCount(taskType);
        int queued = concurrencyControlService.getQueuedTaskCount(taskType);
        int maxConcurrency = concurrencyControlService.getMaxConcurrency(taskType);
        
        return String.format("任务类型 %s - 最大并发: %d, 运行中: %d, 排队中: %d", 
            taskType, maxConcurrency, running, queued);
    }
}