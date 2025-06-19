package com.example.scheduler.service;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.model.TaskStatus;
import com.example.scheduler.repository.TaskExecutionLogRepository;
import com.example.scheduler.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 增强任务服务测试类
 */
@ExtendWith(MockitoExtension.class)
public class EnhancedTaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private TaskExecutionLogRepository taskExecutionLogRepository;
    
    @Mock
    private Scheduler quartzScheduler;
    
    @Mock
    private TaskScheduler taskScheduler;
    
    @InjectMocks
    private EnhancedTaskService enhancedTaskService;
    
    private Task testTask;
    private TaskRequest testTaskRequest;
    
    @BeforeEach
    void setUp() {
        testTask = new Task("Test Task", LocalDateTime.now().plusMinutes(10));
        testTask.setId(1L);
        
        testTaskRequest = new TaskRequest("Test Task", LocalDateTime.now().plusMinutes(10));
        
        // Mock repository calls for init method
        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(Collections.emptyList());
        when(taskRepository.findPendingTasksByScheduledTime(any())).thenReturn(Collections.emptyList());
        when(taskRepository.findPendingCronTasks()).thenReturn(Collections.emptyList());
        
        // Initialize the scheduler field manually since @PostConstruct won't run in unit tests
        enhancedTaskService.init();
        
        // Reset mocks after initialization
        reset(taskRepository, taskExecutionLogRepository, quartzScheduler);
    }
    
    // 功能1：任务持久化与恢复执行测试
    
    @Test
    void givenTaskRequest_whenScheduleTaskWithPersistence_thenTaskSavedToDatabase() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        // When
        Long taskId = enhancedTaskService.scheduleTaskWithPersistence(testTaskRequest);
        
        // Then
        assertNotNull(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }
    
    @Test
    void givenPendingTasks_whenRecoverTasksOnStartup_thenTasksRescheduled() {
        // Given
        List<Task> pendingTasks = Arrays.asList(testTask);
        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(pendingTasks);
        when(taskRepository.findPendingTasksByScheduledTime(any())).thenReturn(pendingTasks);
        when(taskRepository.findPendingCronTasks()).thenReturn(Collections.emptyList());
        
        // When
        enhancedTaskService.recoverTasksOnStartup();
        
        // Then
        verify(taskRepository, times(1)).findByStatus(TaskStatus.PENDING);
        verify(taskRepository, times(1)).findPendingTasksByScheduledTime(any());
        verify(taskRepository, times(1)).findPendingCronTasks();
    }
    
    @Test
    void givenFailedTask_whenRetryTask_thenRetryCountIncremented() {
        // Given
        testTask.setStatus(TaskStatus.FAILED);
        testTask.setRetryCount(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        // When
        enhancedTaskService.retryTask(1L);
        
        // Then
        verify(taskRepository, times(2)).save(any(Task.class)); // Called twice: once for update, once for scheduling
        verify(taskRepository, times(1)).findById(1L);
    }
    
    @Test
    void givenTaskExceedsMaxRetries_whenRetryTask_thenTaskNotRetried() {
        // Given
        testTask.setStatus(TaskStatus.FAILED);
        testTask.setRetryCount(3);
        testTask.setMaxRetries(3);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> enhancedTaskService.retryTask(1L));
    }
    
    // 功能2：Cron表达式支持测试
    
    @Test
    void givenCronExpression_whenScheduleCronTask_thenTaskSavedWithCronExpression() throws Exception {
        // Given
        TaskRequest cronTaskRequest = new TaskRequest("Cron Task", "0 0 9 ? * MON");
        Task expectedTask = new Task("Cron Task", "0 0 9 ? * MON");
        expectedTask.setId(1L);
        when(taskRepository.save(any(Task.class))).thenReturn(expectedTask);
        when(quartzScheduler.scheduleJob(any(), any())).thenReturn(new java.util.Date());
        
        // When
        Long taskId = enhancedTaskService.scheduleCronTask(cronTaskRequest);
        
        // Then
        assertNotNull(taskId);
        verify(taskRepository, times(1)).save(argThat(task -> 
            "0 0 9 ? * MON".equals(task.getCronExpression())));
    }
    
    @Test
    void givenInvalidCronExpression_whenScheduleCronTask_thenThrowException() {
        // Given
        TaskRequest invalidCronTaskRequest = new TaskRequest("Invalid Cron Task", "invalid-cron");
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            enhancedTaskService.scheduleCronTask(invalidCronTaskRequest));
    }
    
    @Test
    void givenCronTask_whenStopCronTask_thenTaskStatusUpdated() throws SchedulerException {
        // Given
        testTask.setCronExpression("0 0 9 * * MON");
        testTask.setStatus(TaskStatus.PENDING);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(quartzScheduler.deleteJob(any())).thenReturn(true);
        
        // When
        enhancedTaskService.stopCronTask(1L);
        
        // Then
        verify(taskRepository, times(1)).save(argThat(task -> 
            task.getStatus() == TaskStatus.CANCELLED));
        verify(quartzScheduler, times(1)).deleteJob(any());
    }
    
    // 功能3：任务执行日志记录与查询测试
    
    @Test
    void givenTaskExecution_whenLogTaskExecution_thenLogSavedToDatabase() {
        // Given
        TaskExecutionLog expectedLog = new TaskExecutionLog(1L, "Test Task", 
            LocalDateTime.now(), TaskStatus.COMPLETED);
        when(taskExecutionLogRepository.save(any(TaskExecutionLog.class))).thenReturn(expectedLog);
        
        // When
        enhancedTaskService.logTaskExecution(1L, "Test Task", TaskStatus.COMPLETED, null, 1000L);
        
        // Then
        verify(taskExecutionLogRepository, times(1)).save(any(TaskExecutionLog.class));
    }
    
    @Test
    void givenTaskId_whenGetTaskExecutionLogs_thenReturnLogs() {
        // Given
        List<TaskExecutionLog> expectedLogs = Arrays.asList(
            new TaskExecutionLog(1L, "Test Task", LocalDateTime.now(), TaskStatus.COMPLETED)
        );
        when(taskExecutionLogRepository.findByTaskIdOrderByExecutionTimeDesc(1L))
            .thenReturn(expectedLogs);
        
        // When
        List<TaskExecutionLog> logs = enhancedTaskService.getTaskExecutionLogs(1L);
        
        // Then
        assertEquals(1, logs.size());
        assertEquals("Test Task", logs.get(0).getTaskName());
    }
    
    @Test
    void givenPaginationRequest_whenGetExecutionLogsWithPagination_thenReturnPagedResults() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<TaskExecutionLog> logs = Arrays.asList(
            new TaskExecutionLog(1L, "Test Task", LocalDateTime.now(), TaskStatus.COMPLETED)
        );
        Page<TaskExecutionLog> expectedPage = new PageImpl<>(logs, pageRequest, 1);
        when(taskExecutionLogRepository.findAll(pageRequest)).thenReturn(expectedPage);
        
        // When
        Page<TaskExecutionLog> result = enhancedTaskService.getExecutionLogsWithPagination(pageRequest);
        
        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
    }
    
    @Test
    void givenTaskStatus_whenGetExecutionLogsByStatus_thenReturnFilteredLogs() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<TaskExecutionLog> failedLogs = Arrays.asList(
            new TaskExecutionLog(1L, "Failed Task", LocalDateTime.now(), TaskStatus.FAILED)
        );
        Page<TaskExecutionLog> expectedPage = new PageImpl<>(failedLogs, pageRequest, 1);
        when(taskExecutionLogRepository.findByExecutionResult(TaskStatus.FAILED, pageRequest))
            .thenReturn(expectedPage);
        
        // When
        Page<TaskExecutionLog> result = enhancedTaskService.getExecutionLogsByStatus(
            TaskStatus.FAILED, pageRequest);
        
        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(TaskStatus.FAILED, result.getContent().get(0).getExecutionResult());
    }
}