package com.example.scheduler.config;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz调度器配置
 */
@Configuration
public class QuartzConfig {
    
    /**
     * 配置Quartz调度器工厂Bean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        
        // 设置调度器名称
        schedulerFactoryBean.setSchedulerName("TaskScheduler");
        
        // 设置启动时自动启动调度器
        schedulerFactoryBean.setAutoStartup(true);
        
        // 设置应用关闭时等待任务完成
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
        
        // 设置关闭超时时间
        schedulerFactoryBean.setStartupDelay(30);
        
        return schedulerFactoryBean;
    }
    
    /**
     * 获取调度器实例
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) {
        return schedulerFactoryBean.getScheduler();
    }
}