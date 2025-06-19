package com.example.scheduler.service;

import com.example.scheduler.model.Task;
import com.example.scheduler.repository.TaskRepository;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring集成的Quartz任务执行类
 */
@Component
public class SpringTaskExecutionJob implements Job {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringTaskExecutionJob.class);
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private EnhancedTaskService enhancedTaskService;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Long taskId = dataMap.getLong("taskId");
        
        logger.info("Executing cron job for task ID: {}", taskId);
        
        try {
            Optional<Task> taskOptional = taskRepository.findById(taskId);
            if (taskOptional.isPresent()) {
                Task task = taskOptional.get();
                enhancedTaskService.executeTask(task);
            } else {
                logger.warn("Task with ID {} not found for cron execution", taskId);
            }
        } catch (Exception e) {
            logger.error("Error executing cron task with ID: {}", taskId, e);
            throw new JobExecutionException("Failed to execute cron task", e);
        }
    }
}