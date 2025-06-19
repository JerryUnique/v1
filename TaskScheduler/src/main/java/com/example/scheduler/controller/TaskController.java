package com.example.scheduler.controller;

import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskStatus;
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

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private EnhancedTaskService enhancedTaskService;

    // ============ 功能1：任务持久化与恢复执行 ============
    
    /**
     * 调度任务（带持久化）
     */
    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleTask(@Valid @RequestBody TaskRequest request) {
        Long taskId = enhancedTaskService.scheduleTaskWithPersistence(request);
        return ResponseEntity.ok("Task scheduled successfully with ID: " + taskId);
    }
    
    /**
     * 重试失败的任务
     */
    @PostMapping("/{taskId}/retry")
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
    @PostMapping("/schedule/cron")
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
    @DeleteMapping("/{taskId}/cron")
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
    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<TaskExecutionLog>> getTaskExecutionLogs(@PathVariable Long taskId) {
        List<TaskExecutionLog> logs = enhancedTaskService.getTaskExecutionLogs(taskId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 分页获取所有执行日志
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsWithPagination(pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据状态获取执行日志
     */
    @GetMapping("/logs/status/{status}")
    public ResponseEntity<Page<TaskExecutionLog>> getExecutionLogsByStatus(
            @PathVariable TaskStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.getExecutionLogsByStatus(status, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据时间范围获取执行日志
     */
    @GetMapping("/logs/timerange")
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
    @GetMapping("/logs/search")
    public ResponseEntity<Page<TaskExecutionLog>> searchExecutionLogsByTaskName(
            @RequestParam String taskName,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskExecutionLog> logs = enhancedTaskService.searchExecutionLogsByTaskName(taskName, pageable);
        return ResponseEntity.ok(logs);
    }
}