package com.example.ojpt.judge;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessJudgeEnvironmentCommandExecutor implements JudgeEnvironmentCommandExecutor {

    @Override
    public CommandResult execute(List<String> command, Duration timeout) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return CommandResult.timeout();
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), stdout, stderr, false);
        } catch (IOException e) {
            return CommandResult.failure(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return CommandResult.failure("Command interrupted");
        }
    }
}
