package com.example.scheduler;

import com.example.scheduler.model.TaskRequest;
import com.example.scheduler.service.TaskService;
import org.junit.jupiter.api.Test;
import java.time.Instant;

public class TaskServiceTest {

    private final TaskService taskService = new TaskService();

    @Test
    public void testScheduleTask() throws InterruptedException {
        taskService.init();
        TaskRequest request = new TaskRequest();
        request.setTaskName("TestTask");
        request.setRunAt(Instant.now().plusSeconds(2));
        taskService.scheduleTask(request);

        Thread.sleep(3000);
    }
}