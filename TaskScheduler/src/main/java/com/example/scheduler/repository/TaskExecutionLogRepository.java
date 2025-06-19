package com.example.scheduler.repository;

import com.example.scheduler.model.TaskExecutionLog;
import com.example.scheduler.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行日志仓储接口
 */
@Repository
public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, Long> {
    
    /**
     * 根据任务ID查找执行日志
     */
    List<TaskExecutionLog> findByTaskIdOrderByExecutionTimeDesc(Long taskId);
    
    /**
     * 根据执行结果查找日志
     */
    Page<TaskExecutionLog> findByExecutionResult(TaskStatus executionResult, Pageable pageable);
    
    /**
     * 根据时间范围查找日志
     */
    @Query("SELECT l FROM TaskExecutionLog l WHERE l.executionTime BETWEEN :startTime AND :endTime ORDER BY l.executionTime DESC")
    Page<TaskExecutionLog> findByExecutionTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime, 
                                                     Pageable pageable);
    
    /**
     * 根据任务名称查找日志
     */
    Page<TaskExecutionLog> findByTaskNameContainingIgnoreCaseOrderByExecutionTimeDesc(String taskName, Pageable pageable);
    
    /**
     * 查找失败的执行日志
     */
    Page<TaskExecutionLog> findByExecutionResultOrderByExecutionTimeDesc(TaskStatus executionResult, Pageable pageable);
}