package com.example.scheduler.service;

import com.example.scheduler.model.TaskRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskService {

    private TaskScheduler scheduler;

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.initialize();
        this.scheduler = threadPoolTaskScheduler;
    }

    public void scheduleTask(TaskRequest request) {
        Instant runAt = request.getRunAt();
        scheduler.schedule(() -> {
            System.out.println("Running task: " + request.getTaskName());
        }, runAt);
    }
}