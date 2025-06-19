package com.example.scheduler.controller;

import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskType;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.service.TaskService;
import com.example.scheduler.service.ConcurrencyControlService;
import com.example.scheduler.service.EnhancedTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*") // 允许跨域访问，便于前端开发
public class TaskController {

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ConcurrencyControlService concurrencyControlService;

    @Autowired
    private EnhancedTaskService enhancedTaskService;

    // ============ 基础任务调度功能 ============

    /**
     * 调度任务（基础版本）
     */
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> scheduleTask(@RequestBody TaskRequest request) {
        try {
            String taskId = taskService.scheduleTask(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "任务已成功调度");
            response.put("taskId", taskId);
            response.put("taskName", request.getTaskName());
            response.put("taskType", request.getTaskType());
            response.put("runAt", request.getRunAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "任务调度失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TaskRequest>> getAllTasks() {
        List<TaskRequest> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TaskRequest> getTaskById(@PathVariable String id) {
        TaskRequest task = taskService.getTaskById(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelTask(@PathVariable String id) {
        boolean cancelled = taskService.cancelTask(id);
        if (cancelled) {
            return ResponseEntity.ok("Task cancelled successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<String> updateTask(@PathVariable String id, @RequestBody TaskRequest request) {
        request.setTaskId(id);
        boolean updated = taskService.updateTask(request);
        if (updated) {
            return ResponseEntity.ok("Task updated successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskRequest>> getTasksByStatus(@PathVariable String status) {
        List<TaskRequest> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 立即执行任务
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@RequestBody TaskRequest request) {
        try {
            taskService.executeTaskImmediately(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "任务已提交执行");
            response.put("taskName", request.getTaskName());
            response.put("taskType", request.getTaskType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "任务执行失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取线程池状态
     */
    @GetMapping("/thread-pool/status")
    public ResponseEntity<Map<String, Object>> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("poolSize", taskService.getThreadPoolSize());
        status.put("activeThreads", taskService.getActiveThreadCount());
        status.put("statusDescription", taskService.getThreadPoolStatus());
        
        return ResponseEntity.ok(status);
    }

    /**
     * 动态调整线程池大小
     */
    @PutMapping("/thread-pool/size")
    public ResponseEntity<Map<String, Object>> updateThreadPoolSize(@RequestParam int newSize) {
        try {
            int oldSize = taskService.getThreadPoolSize();
            taskService.updateThreadPoolSize(newSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "线程池大小已更新");
            response.put("oldSize", oldSize);
            response.put("newSize", newSize);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 设置任务类型的最大并发数
     */
    @PutMapping("/concurrency/{taskType}")
    public ResponseEntity<Map<String, Object>> setTaskTypeConcurrency(
            @PathVariable String taskType,
            @RequestParam int maxConcurrency) {
        try {
            TaskType type = TaskType.valueOf(taskType.toUpperCase());
            taskService.setTaskTypeConcurrency(type, maxConcurrency);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "并发数已更新");
            response.put("taskType", type);
            response.put("maxConcurrency", maxConcurrency);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的任务类型: " + taskType);
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "设置失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取任务类型状态
     */
    @GetMapping("/status/{taskType}")
    public ResponseEntity<Map<String, Object>> getTaskTypeStatus(@PathVariable String taskType) {
        try {
            TaskType type = TaskType.valueOf(taskType.toUpperCase());
            
            Map<String, Object> status = new HashMap<>();
            status.put("taskType", type);
            status.put("maxConcurrency", concurrencyControlService.getMaxConcurrency(type));
            status.put("runningTasks", concurrencyControlService.getRunningTaskCount(type));
            status.put("queuedTasks", concurrencyControlService.getQueuedTaskCount(type));
            status.put("statusDescription", taskService.getTaskTypeStatus(type));
            
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的任务类型: " + taskType);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取所有任务类型的状态
     */
    @GetMapping("/status/all")
    public ResponseEntity<Map<String, Object>> getAllTaskTypesStatus() {
        Map<String, Object> allStatus = new HashMap<>();
        
        for (TaskType taskType : TaskType.values()) {
            Map<String, Object> typeStatus = new HashMap<>();
            typeStatus.put("maxConcurrency", concurrencyControlService.getMaxConcurrency(taskType));
            typeStatus.put("runningTasks", concurrencyControlService.getRunningTaskCount(taskType));
            typeStatus.put("queuedTasks", concurrencyControlService.getQueuedTaskCount(taskType));
            
            allStatus.put(taskType.name(), typeStatus);
        }
        
        return ResponseEntity.ok(allStatus);
    }

    /**
     * 获取系统整体状态
     */
    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> systemStatus = new HashMap<>();
        
        // 线程池状态
        Map<String, Object> threadPoolStatus = new HashMap<>();
        threadPoolStatus.put("poolSize", taskService.getThreadPoolSize());
        threadPoolStatus.put("activeThreads", taskService.getActiveThreadCount());
        
        // 所有任务类型状态
        Map<String, Object> taskTypesStatus = new HashMap<>();
        int totalRunning = 0;
        int totalQueued = 0;
        
        for (TaskType taskType : TaskType.values()) {
            Map<String, Object> typeStatus = new HashMap<>();
            int running = concurrencyControlService.getRunningTaskCount(taskType);
            int queued = concurrencyControlService.getQueuedTaskCount(taskType);
            
            typeStatus.put("maxConcurrency", concurrencyControlService.getMaxConcurrency(taskType));
            typeStatus.put("runningTasks", running);
            typeStatus.put("queuedTasks", queued);
            
            taskTypesStatus.put(taskType.name(), typeStatus);
            totalRunning += running;
            totalQueued += queued;
        }
        
        systemStatus.put("threadPool", threadPoolStatus);
        systemStatus.put("taskTypes", taskTypesStatus);
        systemStatus.put("totalRunningTasks", totalRunning);
        systemStatus.put("totalQueuedTasks", totalQueued);
        
        return ResponseEntity.ok(systemStatus);
    }

    // ============ 增强型任务调度功能 ============

    /**
     * 调度任务（带持久化）
     */
    @PostMapping("/enhanced/schedule")
    public ResponseEntity<String> scheduleEnhancedTask(@Valid @RequestBody TaskRequest request) {
        Long taskId = enhancedTaskService.scheduleTaskWithPersistence(request);
        return ResponseEntity.ok("Task scheduled successfully with ID: " + taskId);
    }
    
    /**
     * 重试失败的任务
     */
    @PostMapping("/enhanced/{taskId}/retry")
    public ResponseEntity<String> retryTask(@PathVariable Long taskId) {
        try {
            enhancedTaskService.retryTask(taskId);
            return ResponseEntity.ok("Task retry scheduled successfully for ID: " + taskId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // ============ 功能2：Cron表达式支持 ============
    
    /**
     * 调度Cron任务
     */
    @PostMapping("/enhanced/schedule/cron")
    public ResponseEntity<String> scheduleCronTask(@Valid @RequestBody TaskRequest request) {
        try {
            Long taskId = enhancedTaskService.scheduleCronTask(request);
            return ResponseEntity.ok("Cron task scheduled successfully with ID: " + taskId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * 停止Cron任务
     */
    @DeleteMapping("/enhanced/{taskId}/cron")
    public ResponseEntity<String> stopCronTask(@PathVariable Long taskId) {
        try {
            enhancedTaskService.stopCronTask(taskId);
            return ResponseEntity.ok("Cron task stopped successfully for ID: " + taskId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    // ============ 功能3：任务执行日志记录与查询 ============
    
    /**
     * 获取任务执行日志
     */
    @GetMapping("/enhanced/{taskId}/logs")
    public ResponseEntity<List<TaskExecutionLog>> getTaskExecutionLogs(@PathVariable Long taskId) {
        List<TaskExecutionLog> logs = enhancedTaskService.getTaskExecutionLogs(taskId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 分页获取所有执行日志
     */
    @GetMapping("/enhanced/logs")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsWithPagination(pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据状态获取执行日志
     */
    @GetMapping("/enhanced/logs/status/{status}")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogsByStatus(
            @PathVariable TaskStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsByStatus(status, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据时间范围获取执行日志
     */
    @GetMapping("/enhanced/logs/timerange")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsByTimeRange(
                startTime, endTime, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据任务名称搜索执行日志
     */
    @GetMapping("/enhanced/logs/search")
    public ResponseEntity<Page<TaskExecutionLog>> searchExecutionLogsByTaskName(
            @RequestParam String taskName,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.searchExecutionLogsByTaskName(taskName, pageable);
        return ResponseEntity.ok(logs);
    }
}