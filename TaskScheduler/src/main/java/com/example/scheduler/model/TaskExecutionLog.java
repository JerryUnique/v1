package com.example.scheduler.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 任务执行日志实体类
 */
@Entity
@Table(name = "task_execution_logs")
public class TaskExecutionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long taskId;
    
    @Column(nullable = false)
    private String taskName;
    
    @Column(nullable = false)
    private LocalDateTime executionTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus executionResult;
    
    @Column(length = 2000)
    private String errorMessage;
    
    @Column
    private Long executionDurationMs;
    
    @Column
    private LocalDateTime createdAt;
    
    // 构造函数
    public TaskExecutionLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    public TaskExecutionLog(Long taskId, String taskName, LocalDateTime executionTime, TaskStatus executionResult) {
        this();
        this.taskId = taskId;
        this.taskName = taskName;
        this.executionTime = executionTime;
        this.executionResult = executionResult;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
    
    public TaskStatus getExecutionResult() {
        return executionResult;
    }
    
    public void setExecutionResult(TaskStatus executionResult) {
        this.executionResult = executionResult;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }
    
    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}