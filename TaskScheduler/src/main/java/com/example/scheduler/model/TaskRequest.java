package com.example.scheduler.model;

import java.time.Instant;

public class TaskRequest {

    private String taskName;
    private Instant runAt;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Instant getRunAt() {
        return runAt;
    }

    public void setRunAt(Instant runAt) {
        this.runAt = runAt;
    }
}