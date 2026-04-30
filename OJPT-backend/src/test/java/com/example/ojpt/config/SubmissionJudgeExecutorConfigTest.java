package com.example.ojpt.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SubmissionJudgeExecutorConfigTest {

    @Test
    void submissionJudgeExecutorUsesBoundedThreadPool() {
        Executor executor = new SubmissionJudgeExecutorConfig().submissionJudgeExecutor();

        ThreadPoolTaskExecutor taskExecutor = assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        assertEquals(2, taskExecutor.getCorePoolSize());
        assertEquals(2, taskExecutor.getMaxPoolSize());
        assertEquals(20, taskExecutor.getQueueCapacity());
    }
}
