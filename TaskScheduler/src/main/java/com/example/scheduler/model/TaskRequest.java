package com.example.scheduler.model;

import javax.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDateTime;

public class TaskRequest {

    private String taskId;
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    private Instant runAt; // 保持原有字段用于向后兼容
    private LocalDateTime scheduledTime;
    private String cronExpression;
    private TaskType taskType = TaskType.DEFAULT;
    private int priority = 0; // 优先级，数值越大优先级越高
    private TaskStatus status = TaskStatus.WAITING;
    private Instant createdAt;
    private Instant updatedAt;
    private String description;
    private Integer maxRetries = 3;

    // 构造函数
    public TaskRequest() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = TaskStatus.WAITING;
    }
    
    public TaskRequest(String taskName, LocalDateTime scheduledTime) {
        this();
        this.taskName = taskName;
        this.scheduledTime = scheduledTime;
    }
    
    public TaskRequest(String taskName, String cronExpression) {
        this();
        this.taskName = taskName;
        this.cronExpression = cronExpression;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
        if (this.updatedAt != null) {
            this.updatedAt = Instant.now();
        }
    }

    public Instant getRunAt() {
        return runAt;
    }

    public void setRunAt(Instant runAt) {
        this.runAt = runAt;
        if (this.updatedAt != null) {
            this.updatedAt = Instant.now();
        }
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        if (this.updatedAt != null) {
            this.updatedAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        if (this.updatedAt != null) {
            this.updatedAt = Instant.now();
        }
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}