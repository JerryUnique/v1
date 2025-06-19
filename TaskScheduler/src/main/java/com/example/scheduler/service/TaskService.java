package com.example.scheduler.service;

import com.example.scheduler.model.TaskRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskService {

    private TaskScheduler scheduler;
    private final Map<String, TaskRequest> tasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.initialize();
        this.scheduler = threadPoolTaskScheduler;
    }

    public String scheduleTask(TaskRequest request) {
        String taskId = generateTaskId();
        request.setTaskId(taskId);
        request.setStatus("SCHEDULED");
        
        Instant runAt = request.getRunAt();
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            executeTask(taskId);
        }, runAt);
        
        tasks.put(taskId, request);
        scheduledTasks.put(taskId, future);
        
        return taskId;
    }
    
    public List<TaskRequest> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    public TaskRequest getTaskById(String taskId) {
        return tasks.get(taskId);
    }
    
    public boolean cancelTask(String taskId) {
        TaskRequest task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        
        task.setStatus("CANCELLED");
        scheduledTasks.remove(taskId);
        
        return true;
    }
    
    public boolean updateTask(TaskRequest updatedRequest) {
        String taskId = updatedRequest.getTaskId();
        TaskRequest existingTask = tasks.get(taskId);
        
        if (existingTask == null) {
            return false;
        }
        
        // Cancel existing scheduled task
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        
        // Update task details
        existingTask.setTaskName(updatedRequest.getTaskName());
        existingTask.setRunAt(updatedRequest.getRunAt());
        existingTask.setDescription(updatedRequest.getDescription());
        existingTask.setStatus("SCHEDULED");
        
        // Reschedule task
        ScheduledFuture<?> newFuture = scheduler.schedule(() -> {
            executeTask(taskId);
        }, updatedRequest.getRunAt());
        
        scheduledTasks.put(taskId, newFuture);
        
        return true;
    }
    
    public List<TaskRequest> getTasksByStatus(String status) {
        return tasks.values().stream()
                .filter(task -> status.equals(task.getStatus()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    private void executeTask(String taskId) {
        TaskRequest task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("RUNNING");
            try {
                System.out.println("Running task: " + task.getTaskName() + " (ID: " + taskId + ")");
                // 模拟任务执行
                Thread.sleep(1000);
                task.setStatus("COMPLETED");
                System.out.println("Task completed: " + task.getTaskName() + " (ID: " + taskId + ")");
            } catch (Exception e) {
                task.setStatus("FAILED");
                System.err.println("Task failed: " + task.getTaskName() + " (ID: " + taskId + ") - " + e.getMessage());
            } finally {
                scheduledTasks.remove(taskId);
            }
        }
    }
    
    private String generateTaskId() {
        return "task-" + System.currentTimeMillis() + "-" + new Random().nextInt(1000);
    }
}