package com.example.scheduler.controller;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskType;
import com.example.scheduler.service.TaskService;
import com.example.scheduler.service.ConcurrencyControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 调度任务
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
}