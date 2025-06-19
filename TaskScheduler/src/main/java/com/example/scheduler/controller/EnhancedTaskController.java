package com.example.scheduler.controller;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.service.EnhancedTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强的任务控制器
 * 提供任务持久化、Cron表达式支持和日志记录功能的API接口
 */
@RestController
@RequestMapping("/api/v2/tasks")
@CrossOrigin(origins = "*")
public class EnhancedTaskController {
    
    @Autowired
    private EnhancedTaskService enhancedTaskService;
    
    // ============ 功能1：任务持久化与恢复执行 API ============
    
    /**
     * 调度任务并持久化
     */
    @PostMapping("/schedule")
    public ResponseEntity<Map<String, Object>> scheduleTask(@Valid @RequestBody TaskRequest request) {
        try {
            Long taskId = enhancedTaskService.scheduleTaskWithPersistence(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Task scheduled successfully");
            response.put("taskName", request.getTaskName());
            response.put("scheduledTime", request.getScheduledTime());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 重试失败的任务
     */
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable Long taskId) {
        try {
            enhancedTaskService.retryTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task scheduled for retry");
            response.put("taskId", taskId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Task not found: " + taskId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retry task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 手动触发任务恢复
     */
    @PostMapping("/recover")
    public ResponseEntity<Map<String, Object>> recoverTasks() {
        try {
            enhancedTaskService.recoverTasksOnStartup();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tasks recovery completed");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to recover tasks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============ 功能2：Cron表达式支持 API ============
    
    /**
     * 调度Cron任务
     */
    @PostMapping("/schedule/cron")
    public ResponseEntity<Map<String, Object>> scheduleCronTask(@Valid @RequestBody TaskRequest request) {
        try {
            Long taskId = enhancedTaskService.scheduleCronTask(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "Cron task scheduled successfully");
            response.put("taskName", request.getTaskName());
            response.put("cronExpression", request.getCronExpression());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to schedule cron task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 停止Cron任务
     */
    @PostMapping("/{taskId}/stop")
    public ResponseEntity<Map<String, Object>> stopCronTask(@PathVariable Long taskId) {
        try {
            enhancedTaskService.stopCronTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cron task stopped successfully");
            response.put("taskId", taskId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Task not found: " + taskId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to stop cron task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ============ 功能3：任务执行日志记录与查询 API ============
    
    /**
     * 获取任务执行日志
     */
    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<TaskExecutionLog>> getTaskExecutionLogs(@PathVariable Long taskId) {
        try {
            List<TaskExecutionLog> logs = enhancedTaskService.getTaskExecutionLogs(taskId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 分页获取所有执行日志
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsWithPagination(pageable);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据状态获取执行日志
     */
    @GetMapping("/logs/status/{status}")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogsByStatus(
            @PathVariable TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsByStatus(status, pageable);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据时间范围获取执行日志
     */
    @GetMapping("/logs/range")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsByTimeRange(
                startTime, endTime, pageable);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 根据任务名称搜索执行日志
     */
    @GetMapping("/logs/search")
    public ResponseEntity<Page<TaskExecutionLog>> searchExecutionLogsByTaskName(
            @RequestParam String taskName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TaskExecutionLog> logs = enhancedTaskService.searchExecutionLogsByTaskName(
                taskName, pageable);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ============ 辅助API ============
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "Enhanced Task Scheduler");
        return ResponseEntity.ok(health);
    }
    
    /**
     * 获取API文档说明
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "2.0");
        info.put("features", new String[]{
            "Task Persistence and Recovery",
            "Cron Expression Support", 
            "Task Execution Logging"
        });
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/v2/tasks/schedule", "Schedule a one-time task with persistence");
        endpoints.put("POST /api/v2/tasks/schedule/cron", "Schedule a recurring cron task");
        endpoints.put("POST /api/v2/tasks/{id}/retry", "Retry a failed task");
        endpoints.put("POST /api/v2/tasks/{id}/stop", "Stop a cron task");
        endpoints.put("GET /api/v2/tasks/{id}/logs", "Get execution logs for a specific task");
        endpoints.put("GET /api/v2/tasks/logs", "Get all execution logs (paginated)");
        endpoints.put("GET /api/v2/tasks/logs/status/{status}", "Get execution logs by status");
        endpoints.put("GET /api/v2/tasks/logs/range", "Get execution logs by time range");
        endpoints.put("GET /api/v2/tasks/logs/search", "Search execution logs by task name");
        
        info.put("endpoints", endpoints);
        return ResponseEntity.ok(info);
    }
}