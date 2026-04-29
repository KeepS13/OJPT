package com.example.ojpt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class SubmissionJudgeExecutorConfig {

    @Bean(name = "submissionJudgeExecutor")
    public Executor submissionJudgeExecutor() {
        return Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("ojpt-judge-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }
}
