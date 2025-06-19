package com.example.scheduler.model;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    /**
     * 等待执行
     */
    PENDING("等待执行"),
    
    /**
     * 运行中
     */
    RUNNING("执行中"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成"),
    
    /**
     * 失败
     */
    FAILED("执行失败"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String description;

    TaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}