package com.example.scheduler.service;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskType;
import com.example.scheduler.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发控制服务
 * 负责管理不同类型任务的并发执行数量和排队机制
 */
@Service
public class ConcurrencyControlService {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyControlService.class);

    // 每种任务类型的信号量映射
    private final Map<TaskType, Semaphore> semaphoreMap = new ConcurrentHashMap<>();
    
    // 每种任务类型的最大并发数配置
    private final Map<TaskType, Integer> maxConcurrencyMap = new ConcurrentHashMap<>();
    
    // 每种任务类型的任务队列
    private final Map<TaskType, BlockingQueue<TaskRequest>> taskQueues = new ConcurrentHashMap<>();
    
    // 每种任务类型的运行中任务计数
    private final Map<TaskType, AtomicInteger> runningTaskCounts = new ConcurrentHashMap<>();
    
    // 队列处理线程池
    private final ExecutorService queueProcessorExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "task-queue-processor");
        t.setDaemon(true);
        return t;
    });

    /**
     * 设置指定任务类型的最大并发数
     */
    public void setMaxConcurrency(TaskType taskType, int maxConcurrency) {
        maxConcurrencyMap.put(taskType, maxConcurrency);
        semaphoreMap.put(taskType, new Semaphore(maxConcurrency, true));
        taskQueues.putIfAbsent(taskType, new PriorityBlockingQueue<>(11, 
            (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority())));
        runningTaskCounts.putIfAbsent(taskType, new AtomicInteger(0));
        
        // 启动队列处理器
        startQueueProcessor(taskType);
        
        logger.info("设置任务类型 {} 的最大并发数为: {}", taskType, maxConcurrency);
    }

    /**
     * 获取指定任务类型的最大并发数
     */
    public int getMaxConcurrency(TaskType taskType) {
        return maxConcurrencyMap.getOrDefault(taskType, 1);
    }

    /**
     * 使用并发控制执行任务
     */
    public void executeWithConcurrencyControl(TaskRequest taskRequest, Runnable taskLogic) {
        TaskType taskType = taskRequest.getTaskType();
        
        // 确保该任务类型已配置
        ensureTaskTypeConfigured(taskType);
        
        Semaphore semaphore = semaphoreMap.get(taskType);
        
        // 尝试获取许可证
        if (semaphore.tryAcquire()) {
            // 直接执行任务
            executeTask(taskRequest, taskLogic, semaphore);
        } else {
            // 添加到队列
            taskRequest.setStatus(TaskStatus.QUEUED);
            BlockingQueue<TaskRequest> queue = taskQueues.get(taskType);
            queue.offer(taskRequest);
            logger.info("任务 {} 已添加到队列，当前队列大小: {}", 
                taskRequest.getTaskName(), queue.size());
        }
    }

    /**
     * 获取指定任务类型的运行中任务数量
     */
    public int getRunningTaskCount(TaskType taskType) {
        AtomicInteger counter = runningTaskCounts.get(taskType);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取指定任务类型的排队任务数量
     */
    public int getQueuedTaskCount(TaskType taskType) {
        BlockingQueue<TaskRequest> queue = taskQueues.get(taskType);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 执行任务
     */
    private void executeTask(TaskRequest taskRequest, Runnable taskLogic, Semaphore semaphore) {
        TaskType taskType = taskRequest.getTaskType();
        AtomicInteger runningCount = runningTaskCounts.get(taskType);
        
        try {
            taskRequest.setStatus(TaskStatus.RUNNING);
            runningCount.incrementAndGet();
            
            logger.info("开始执行任务: {}, 类型: {}", taskRequest.getTaskName(), taskType);
            
            // 执行实际任务逻辑
            taskLogic.run();
            
            taskRequest.setStatus(TaskStatus.COMPLETED);
            logger.info("任务执行完成: {}", taskRequest.getTaskName());
            
        } catch (Exception e) {
            taskRequest.setStatus(TaskStatus.FAILED);
            logger.error("任务执行失败: {}, 错误: {}", taskRequest.getTaskName(), e.getMessage(), e);
        } finally {
            // 释放许可证
            semaphore.release();
            runningCount.decrementAndGet();
            logger.info("释放任务类型 {} 的许可证，剩余许可: {}", taskType, semaphore.availablePermits());
        }
    }

    /**
     * 启动队列处理器
     */
    private void startQueueProcessor(TaskType taskType) {
        queueProcessorExecutor.submit(() -> {
            BlockingQueue<TaskRequest> queue = taskQueues.get(taskType);
            Semaphore semaphore = semaphoreMap.get(taskType);
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 等待队列中的任务
                    TaskRequest taskRequest = queue.take();
                    
                    // 等待获取许可证
                    semaphore.acquire();
                    
                    // 在新线程中执行任务
                    CompletableFuture.runAsync(() -> {
                        executeTask(taskRequest, () -> {
                            // 这里应该从原始任务逻辑中获取，暂时用空实现
                            try {
                                Thread.sleep(100); // 模拟任务执行
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }, semaphore);
                    });
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * 确保任务类型已配置
     */
    private void ensureTaskTypeConfigured(TaskType taskType) {
        if (!maxConcurrencyMap.containsKey(taskType)) {
            // 使用默认配置
            setMaxConcurrency(taskType, 1);
        }
    }
}