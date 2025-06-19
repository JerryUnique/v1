package com.example.scheduler.model;

import javax.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDateTime;

public class TaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    
    private String description;
    
    private Instant runAt; // 保持原有字段用于向后兼容
    
    private LocalDateTime scheduledTime;
    
    private String cronExpression;
    
    private Integer maxRetries = 3;

    // 构造函数
    public TaskRequest() {}
    
    public TaskRequest(String taskName, LocalDateTime scheduledTime) {
        this.taskName = taskName;
        this.scheduledTime = scheduledTime;
    }
    
    public TaskRequest(String taskName, String cronExpression) {
        this.taskName = taskName;
        this.cronExpression = cronExpression;
    }

    // Getters and Setters
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public void setRunAt(Instant runAt) {
        this.runAt = runAt;
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