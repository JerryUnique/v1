package com.example.scheduler.model;

import java.time.Instant;

public class TaskRequest {

    private String taskId;
    private String taskName;
    private Instant runAt;
    private String status; // SCHEDULED, RUNNING, COMPLETED, CANCELLED, FAILED
    private Instant createdAt;
    private Instant updatedAt;
    private String description;

    public TaskRequest() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = "SCHEDULED";
    }

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
        this.updatedAt = Instant.now();
    }

    public Instant getRunAt() {
        return runAt;
    }

    public void setRunAt(Instant runAt) {
        this.runAt = runAt;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
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
        this.updatedAt = Instant.now();
    }
}