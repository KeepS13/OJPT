package com.example.ojpt.judge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeExecutionResult {
    private boolean compileSuccess;
    private boolean runtimeSuccess;
    private boolean timedOut;
    private String stdout;
    private String stderr;
    private Long timeMs;
}
