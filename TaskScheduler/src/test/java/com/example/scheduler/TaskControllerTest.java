package com.example.scheduler;

import com.example.scheduler.controller.TaskController;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Given valid task request When schedule task via POST Then should return success message")
    public void given_validTaskRequest_when_scheduleTaskViaPost_then_shouldReturnSuccessMessage() throws Exception {
        // Given
        TaskRequest request = new TaskRequest();
        request.setTaskName("TestTask");
        request.setRunAt(Instant.now().plusSeconds(10));
        
        when(taskService.scheduleTask(any(TaskRequest.class))).thenReturn("task-123");

        // When & Then
        mockMvc.perform(post("/tasks/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Task scheduled successfully with ID: task-123"));
    }

    @Test
    @DisplayName("Given existing tasks When get all tasks Then should return task list")
    public void given_existingTasks_when_getAllTasks_then_shouldReturnTaskList() throws Exception {
        // Given
        TaskRequest task1 = new TaskRequest();
        task1.setTaskId("task-1");
        task1.setTaskName("Task1");
        task1.setStatus("SCHEDULED");

        TaskRequest task2 = new TaskRequest();
        task2.setTaskId("task-2");
        task2.setTaskName("Task2");
        task2.setStatus("RUNNING");

        List<TaskRequest> tasks = Arrays.asList(task1, task2);
        when(taskService.getAllTasks()).thenReturn(tasks);

        // When & Then
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].taskId").value("task-1"))
                .andExpect(jsonPath("$[0].taskName").value("Task1"))
                .andExpect(jsonPath("$[1].taskId").value("task-2"))
                .andExpect(jsonPath("$[1].taskName").value("Task2"));
    }

    @Test
    @DisplayName("Given existing task id When get task by id Then should return task details")
    public void given_existingTaskId_when_getTaskById_then_shouldReturnTaskDetails() throws Exception {
        // Given
        String taskId = "task-123";
        TaskRequest task = new TaskRequest();
        task.setTaskId(taskId);
        task.setTaskName("TestTask");
        task.setStatus("SCHEDULED");
        
        when(taskService.getTaskById(taskId)).thenReturn(task);

        // When & Then
        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.taskName").value("TestTask"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Given existing task When delete task Then should return success message")
    public void given_existingTask_when_deleteTask_then_shouldReturnSuccessMessage() throws Exception {
        // Given
        String taskId = "task-123";
        when(taskService.cancelTask(taskId)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(content().string("Task cancelled successfully"));
    }

    @Test
    @DisplayName("Given non-existent task When delete task Then should return not found")
    public void given_nonExistentTask_when_deleteTask_then_shouldReturnNotFound() throws Exception {
        // Given
        String taskId = "non-existent-task";
        when(taskService.cancelTask(taskId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Task not found"));
    }

    @Test
    @DisplayName("Given task update request When update task Then should return success message")
    public void given_taskUpdateRequest_when_updateTask_then_shouldReturnSuccessMessage() throws Exception {
        // Given
        String taskId = "task-123";
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setTaskId(taskId);
        updateRequest.setTaskName("UpdatedTask");
        updateRequest.setRunAt(Instant.now().plusSeconds(20));
        
        when(taskService.updateTask(any(TaskRequest.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Task updated successfully"));
    }
}