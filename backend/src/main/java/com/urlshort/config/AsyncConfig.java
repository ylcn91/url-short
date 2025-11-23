package com.urlshort.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * <p>
 * This configuration enables Spring's @Async annotation and configures a thread pool
 * for handling asynchronous tasks like click event recording.
 * </p>
 *
 * <h3>Async Tasks:</h3>
 * <ul>
 *   <li><b>Click Event Recording:</b> Non-blocking analytics tracking</li>
 *   <li><b>Email Notifications:</b> Asynchronous email sending</li>
 *   <li><b>Batch Processing:</b> Background jobs and data processing</li>
 * </ul>
 *
 * <h3>Thread Pool Configuration:</h3>
 * <ul>
 *   <li><b>Core Pool Size:</b> 2 threads (minimum active threads)</li>
 *   <li><b>Max Pool Size:</b> 10 threads (maximum concurrent threads)</li>
 *   <li><b>Queue Capacity:</b> 500 tasks (pending task queue)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates and configures the thread pool executor for async tasks.
     * <p>
     * The executor uses a bounded queue to prevent memory issues and
     * gracefully rejects tasks when the queue is full.
     * </p>
     *
     * @return configured thread pool task executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("Initializing async task executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum number of threads
        executor.setCorePoolSize(2);

        // Maximum pool size - max concurrent threads
        executor.setMaxPoolSize(10);

        // Queue capacity - pending tasks queue
        executor.setQueueCapacity(500);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("async-task-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Shutdown timeout
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Async task executor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
