package com.example.scheduler.service;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskType;
import com.example.scheduler.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发控制测试类
 * 采用 give_when_then 模式编写测试用例
 */
@SpringBootTest
public class ConcurrencyControlTest {

    private ConcurrencyControlService concurrencyControlService;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        // Given: 初始化并发控制服务
        concurrencyControlService = new ConcurrencyControlService();
        taskService = new TaskService();
        taskService.init();
    }

    @Test
    @DisplayName("给定数据处理任务最大并发数为2，当同时提交3个数据处理任务时，应该有1个任务排队")
    void given_dataProcessingMaxConcurrency2_when_submit3DataProcessingTasks_then_1TaskShouldBeQueued() 
            throws InterruptedException {
        // Given: 数据处理任务最大并发数为2
        int maxConcurrency = 2;
        concurrencyControlService.setMaxConcurrency(TaskType.DATA_PROCESSING, maxConcurrency);
        
        // When: 同时提交3个数据处理任务
        List<TaskRequest> tasks = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            TaskRequest task = createTaskRequest("DataTask-" + i, TaskType.DATA_PROCESSING);
            tasks.add(task);
        }
        
        // 并发提交任务
        tasks.forEach(task -> {
            new Thread(() -> {
                try {
                    startLatch.await();
                    concurrencyControlService.executeWithConcurrencyControl(task, () -> {
                        try {
                            Thread.sleep(1000); // 模拟任务执行
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        doneLatch.countDown();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
        
        startLatch.countDown(); // 开始执行
        
        // Then: 应该有1个任务排队
        Thread.sleep(100); // 等待任务开始执行
        int runningTasks = concurrencyControlService.getRunningTaskCount(TaskType.DATA_PROCESSING);
        int queuedTasks = concurrencyControlService.getQueuedTaskCount(TaskType.DATA_PROCESSING);
        
        assertEquals(maxConcurrency, runningTasks, "运行中的任务数应该等于最大并发数");
        assertEquals(1, queuedTasks, "应该有1个任务排队");
        
        // 等待所有任务完成
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "所有任务应该在5秒内完成");
    }

    @Test
    @DisplayName("给定不同类型任务有不同的并发控制，当同时提交不同类型任务时，应该相互独立")
    void given_differentTaskTypesWithDifferentConcurrency_when_submitDifferentTasks_then_shouldBeIndependent() 
            throws InterruptedException {
        // Given: 不同类型任务的并发控制配置
        concurrencyControlService.setMaxConcurrency(TaskType.DATA_PROCESSING, 1);
        concurrencyControlService.setMaxConcurrency(TaskType.REPORT_GENERATION, 2);
        
        // When: 同时提交不同类型的任务
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // 提交2个数据处理任务
        TaskRequest dataTask1 = createTaskRequest("DataTask-1", TaskType.DATA_PROCESSING);
        TaskRequest dataTask2 = createTaskRequest("DataTask-2", TaskType.DATA_PROCESSING);
        
        // 提交2个报表生成任务
        TaskRequest reportTask1 = createTaskRequest("ReportTask-1", TaskType.REPORT_GENERATION);
        TaskRequest reportTask2 = createTaskRequest("ReportTask-2", TaskType.REPORT_GENERATION);
        
        // 并发提交任务
        submitTaskAsync(dataTask1, startLatch);
        submitTaskAsync(dataTask2, startLatch);
        submitTaskAsync(reportTask1, startLatch);
        submitTaskAsync(reportTask2, startLatch);
        
        startLatch.countDown(); // 开始执行
        Thread.sleep(100); // 等待任务开始执行
        
        // Then: 数据处理任务应该有1个运行，1个排队；报表任务应该都在运行
        assertEquals(1, concurrencyControlService.getRunningTaskCount(TaskType.DATA_PROCESSING));
        assertEquals(1, concurrencyControlService.getQueuedTaskCount(TaskType.DATA_PROCESSING));
        assertEquals(2, concurrencyControlService.getRunningTaskCount(TaskType.REPORT_GENERATION));
        assertEquals(0, concurrencyControlService.getQueuedTaskCount(TaskType.REPORT_GENERATION));
    }

    @Test
    @DisplayName("给定任务执行失败，当释放许可证时，排队的任务应该能够开始执行")
    void given_taskExecutionFails_when_releasePermit_then_queuedTaskShouldStart() 
            throws InterruptedException {
        // Given: 最大并发数为1
        concurrencyControlService.setMaxConcurrency(TaskType.EMAIL_SENDING, 1);
        
        // When: 提交一个会失败的任务和一个正常任务
        TaskRequest failingTask = createTaskRequest("FailingTask", TaskType.EMAIL_SENDING);
        TaskRequest normalTask = createTaskRequest("NormalTask", TaskType.EMAIL_SENDING);
        
        CountDownLatch failTaskStarted = new CountDownLatch(1);
        CountDownLatch normalTaskCompleted = new CountDownLatch(1);
        
        // 提交失败任务
        new Thread(() -> {
            concurrencyControlService.executeWithConcurrencyControl(failingTask, () -> {
                failTaskStarted.countDown();
                throw new RuntimeException("任务执行失败");
            });
        }).start();
        
        // 等待失败任务开始
        assertTrue(failTaskStarted.await(1, TimeUnit.SECONDS));
        
        // 提交正常任务
        new Thread(() -> {
            concurrencyControlService.executeWithConcurrencyControl(normalTask, () -> {
                normalTaskCompleted.countDown();
            });
        }).start();
        
        // Then: 正常任务应该能够执行完成
        assertTrue(normalTaskCompleted.await(3, TimeUnit.SECONDS), "正常任务应该能够执行完成");
    }

    @Test
    @DisplayName("给定线程池配置，当动态调整线程池参数时，新配置应该生效")
    void given_threadPoolConfig_when_dynamicallyAdjustParameters_then_newConfigShouldTakeEffect() {
        // Given: 初始线程池配置
        int initialPoolSize = taskService.getThreadPoolSize();
        
        // When: 动态调整线程池大小
        int newPoolSize = initialPoolSize + 5;
        taskService.updateThreadPoolSize(newPoolSize);
        
        // Then: 新的线程池大小应该生效
        assertEquals(newPoolSize, taskService.getThreadPoolSize(), "线程池大小应该更新为新值");
    }

    // 辅助方法
    private TaskRequest createTaskRequest(String taskName, TaskType taskType) {
        TaskRequest request = new TaskRequest();
        request.setTaskName(taskName);
        request.setTaskType(taskType);
        request.setRunAt(Instant.now());
        return request;
    }
    
    private void submitTaskAsync(TaskRequest task, CountDownLatch startLatch) {
        new Thread(() -> {
            try {
                startLatch.await();
                concurrencyControlService.executeWithConcurrencyControl(task, () -> {
                    try {
                        Thread.sleep(1000); // 模拟任务执行
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}