package com.gymtracker.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables asynchronous execution support for background service tasks
 * (fatigue/1RM calculation, alert generation, report export).
 * <p>
 * Without a custom {@link Executor} bean, Spring falls back to
 * {@code SimpleAsyncTaskExecutor}, which spawns an unbounded new thread per
 * invocation instead of reusing a pool. This configures a bounded pool so
 * async load is capped instead of growing without limit under concurrent use.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final int QUEUE_CAPACITY = 200;

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("gymtracker-async-");
        executor.initialize();
        return executor;
    }
}
