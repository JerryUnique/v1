package com.example.scheduler.controller;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/schedule")
    public String scheduleTask(@RequestBody TaskRequest request) {
        taskService.scheduleTask(request);
        return "Task scheduled at " + request.getRunAt();
    }
}