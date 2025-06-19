package com.example.shceduler;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    public void setUp() {
        taskService = new TaskService();
        taskService.init();
    }

    @Test
    @DisplayName("Given valid task request When schedule task Then task should be scheduled successfully")
    public void given_validTaskRequest_when_scheduleTask_then_taskShouldBeScheduledSuccessfully() throws InterruptedException {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTaskName("TestTask");
        request.setRunAt(Instant.now().plusSeconds(2));

        // When
        String taskId = taskService.scheduleTask(request);

        // Then
        assertNotNull(taskId);
        assertFalse(taskId.isEmpty());
        
        Thread.sleep(3000);
    }

    @Test
    @DisplayName("Given existing task id When get task by id Then should return correct task")
    public void given_existingTaskId_when_getTaskById_then_shouldReturnCorrectTask() {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTaskName("TestTask");
        request.setRunAt(Instant.now().plusSeconds(10));
        String taskId = taskService.scheduleTask(request);

        // When
        TaskRequest retrievedTask = taskService.getTaskById(taskId);

        // Then
        assertNotNull(retrievedTask);
        assertEquals("TestTask", retrievedTask.getTaskName());
        assertEquals(taskId, retrievedTask.getTaskId());
    }

    @Test
    @DisplayName("Given no tasks exist When get all tasks Then should return empty list")
    public void given_noTasksExist_when_getAllTasks_then_shouldReturnEmptyList() {
        // Given - no tasks scheduled

        // When
        List<TaskRequest> tasks = taskService.getAllTasks();

        // Then
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    @DisplayName("Given scheduled task When cancel task Then task should be cancelled")
    public void given_scheduledTask_when_cancelTask_then_taskShouldBeCancelled() {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTaskName("TestTask");
        request.setRunAt(Instant.now().plusSeconds(10));
        String taskId = taskService.scheduleTask(request);

        // When
        boolean cancelled = taskService.cancelTask(taskId);

        // Then
        assertTrue(cancelled);
        TaskRequest cancelledTask = taskService.getTaskById(taskId);
        assertEquals("CANCELLED", cancelledTask.getStatus());
    }

    @Test
    @DisplayName("Given non-existent task id When cancel task Then should return false")
    public void given_nonExistentTaskId_when_cancelTask_then_shouldReturnFalse() {
        // Given
        String nonExistentTaskId = "non-existent-id";

        // When
        boolean cancelled = taskService.cancelTask(nonExistentTaskId);

        // Then
        assertFalse(cancelled);
    }

    @Test
    @DisplayName("Given existing task When update task Then task should be updated")
    public void given_existingTask_when_updateTask_then_taskShouldBeUpdated() {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTaskName("OriginalTask");
        request.setRunAt(Instant.now().plusSeconds(10));
        String taskId = taskService.scheduleTask(request);

        // When
        TaskRequest updatedRequest = new TaskRequest();
        updatedRequest.setTaskId(taskId);
        updatedRequest.setTaskName("UpdatedTask");
        updatedRequest.setRunAt(Instant.now().plusSeconds(20));
        
        boolean updated = taskService.updateTask(updatedRequest);

        // Then
        assertTrue(updated);
        TaskRequest retrievedTask = taskService.getTaskById(taskId);
        assertEquals("UpdatedTask", retrievedTask.getTaskName());
    }
}