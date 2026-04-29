package com.example.ojpt.judge;

import java.time.Duration;
import java.util.List;

public interface JudgeEnvironmentCommandExecutor {

    CommandResult execute(List<String> command, Duration timeout);
}
