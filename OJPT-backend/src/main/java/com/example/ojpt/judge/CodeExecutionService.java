package com.example.ojpt.judge;

public interface CodeExecutionService {
    CodeExecutionResult execute(String language, String sourceCode, String inputText, Integer timeLimitMs, Integer memoryLimitKb);
}
