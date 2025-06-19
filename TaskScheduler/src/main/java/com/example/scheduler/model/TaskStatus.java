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
     * 等待执行
     */
    PENDING("pending", "等待执行"),
    
    /**
     * 排队中
     */
    QUEUED("queued", "排队中"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),
    
    /**
     * 执行失败
     */
    FAILED("failed", "执行失败"),
    
    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消"),
    
    /**
     * 已调度
     */
    SCHEDULED("scheduled", "已调度");
    
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