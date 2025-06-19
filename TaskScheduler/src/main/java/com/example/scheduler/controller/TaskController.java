package com.example.scheduler.controller;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*") // 允许跨域访问，便于前端开发
public class TaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleTask(@RequestBody TaskRequest request) {
        String taskId = taskService.scheduleTask(request);
        return ResponseEntity.ok("Task scheduled successfully with ID: " + taskId);
    }
    
    @GetMapping
    public ResponseEntity<List<TaskRequest>> getAllTasks() {
        List<TaskRequest> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TaskRequest> getTaskById(@PathVariable String id) {
        TaskRequest task = taskService.getTaskById(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelTask(@PathVariable String id) {
        boolean cancelled = taskService.cancelTask(id);
        if (cancelled) {
            return ResponseEntity.ok("Task cancelled successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<String> updateTask(@PathVariable String id, @RequestBody TaskRequest request) {
        request.setTaskId(id);
        boolean updated = taskService.updateTask(request);
        if (updated) {
            return ResponseEntity.ok("Task updated successfully");
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskRequest>> getTasksByStatus(@PathVariable String status) {
        List<TaskRequest> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }
}