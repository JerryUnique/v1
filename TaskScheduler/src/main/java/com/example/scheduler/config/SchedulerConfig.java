package com.example.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
    }
}


// -------------------- SchedulerConfig.java --------------------

package com.example.scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
}