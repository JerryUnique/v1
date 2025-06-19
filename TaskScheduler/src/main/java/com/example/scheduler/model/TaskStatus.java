package com.example.scheduler.model;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    
    /**
     * 等待调度
     */
    WAITING("waiting", "等待调度"),
    
    /**
     * 排队中
     */
    QUEUED("queued", "排队中"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 执行完成
     */
    COMPLETED("completed", "执行完成"),
    
    /**
     * 执行失败
     */
    FAILED("failed", "执行失败");
    
    private final String code;
    private final String description;
    
    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}