package com.example.scheduler.repository;

import com.example.scheduler.model.Task;
import com.example.scheduler.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务仓储接口
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    /**
     * 查找指定状态的任务
     */
    List<Task> findByStatus(TaskStatus status);
    
    /**
     * 查找需要执行的任务（按时间调度）
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'PENDING' AND t.scheduledTime <= :currentTime AND t.cronExpression IS NULL")
    List<Task> findPendingTasksByScheduledTime(LocalDateTime currentTime);
    
    /**
     * 查找Cron任务
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'PENDING' AND t.cronExpression IS NOT NULL")
    List<Task> findPendingCronTasks();
    
    /**
     * 查找失败的需要重试的任务
     */
    @Query("SELECT t FROM Task t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetries")
    List<Task> findFailedTasksForRetry();
}