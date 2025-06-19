package com.example.scheduler.model;

/**
 * 任务类型枚举
 * 用于对任务进行分类，每种类型可以配置不同的并发控制策略
 */
public enum TaskType {
    
    /**
     * 数据处理任务
     */
    DATA_PROCESSING("data_processing", "数据处理任务"),
    
    /**
     * 报表生成任务
     */
    REPORT_GENERATION("report_generation", "报表生成任务"),
    
    /**
     * 文件上传任务
     */
    FILE_UPLOAD("file_upload", "文件上传任务"),
    
    /**
     * 邮件发送任务
     */
    EMAIL_SENDING("email_sending", "邮件发送任务"),
    
    /**
     * 默认任务类型
     */
    DEFAULT("default", "默认任务");
    
    private final String code;
    private final String description;
    
    TaskType(String code, String description) {
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