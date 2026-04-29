package com.example.ojpt.judge;

import com.example.ojpt.dto.JudgeEnvironmentHealthDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeEnvironmentHealthServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void checkHealth_returnsUpWhenDockerAndConfiguredImagesAreAvailable() throws IOException {
        Path docker = Files.createFile(tempDir.resolve("docker.exe"));
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(CommandResult.success("Docker version 26.0.0"));
        executor.enqueue(CommandResult.success("Server Version: 26.0.0"));
        executor.enqueue(CommandResult.success("[]"));
        executor.enqueue(CommandResult.success("[]"));
        executor.enqueue(CommandResult.success("[]"));

        JudgeEnvironmentHealthService service = newService(docker, executor);

        JudgeEnvironmentHealthDTO health = service.checkHealth();

        assertEquals("UP", health.status());
        assertEquals("Judge Docker environment is healthy", health.message());
        assertEquals(6, health.checks().size());
        assertTrue(health.checks().stream().allMatch(check -> "UP".equals(check.status())));
        assertEquals(List.of(docker.toString(), "version"), executor.commands.get(0));
        assertEquals(List.of(docker.toString(), "info"), executor.commands.get(1));
        assertEquals(List.of(docker.toString(), "image", "inspect", "gcc:13.2.0"), executor.commands.get(2));
        assertEquals(List.of(docker.toString(), "image", "inspect", "eclipse-temurin:17-jdk"), executor.commands.get(3));
        assertEquals(List.of(docker.toString(), "image", "inspect", "python:3.11"), executor.commands.get(4));
        assertTrue(executor.timeouts.stream().allMatch(Duration.ofMillis(1234)::equals));
    }

    @Test
    void checkHealth_returnsDownAndSkipsCommandsWhenDockerExecutableIsMissing() {
        Path missingDocker = tempDir.resolve("missing-docker.exe");
        RecordingExecutor executor = new RecordingExecutor();

        JudgeEnvironmentHealthDTO health = newService(missingDocker, executor).checkHealth();

        assertEquals("DOWN", health.status());
        assertEquals(6, health.checks().size());
        assertEquals("DOWN", health.checks().get(0).status());
        assertEquals("SKIPPED", health.checks().get(1).status());
        assertEquals("SKIPPED", health.checks().get(5).status());
        assertTrue(executor.commands.isEmpty());
    }

    @Test
    void checkHealth_returnsDownWhenConfiguredImageIsNotAvailable() throws IOException {
        Path docker = Files.createFile(tempDir.resolve("docker.exe"));
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(CommandResult.success("Docker version 26.0.0"));
        executor.enqueue(CommandResult.success("Server Version: 26.0.0"));
        executor.enqueue(CommandResult.success("[]"));
        executor.enqueue(new CommandResult(1, "", "No such image", false));
        executor.enqueue(CommandResult.success("[]"));

        JudgeEnvironmentHealthDTO health = newService(docker, executor).checkHealth();

        assertEquals("DOWN", health.status());
        assertEquals("Some judge Docker environment checks failed", health.message());
        assertEquals("DOWN", health.checks().get(4).status());
        assertEquals("java", health.checks().get(4).target());
        assertTrue(health.checks().get(4).message().contains("No such image"));
    }

    private JudgeEnvironmentHealthService newService(Path docker, RecordingExecutor executor) {
        return new JudgeEnvironmentHealthService(
                executor,
                docker.toString(),
                "gcc:13.2.0",
                "eclipse-temurin:17-jdk",
                "python:3.11",
                1234L
        );
    }

    private static class RecordingExecutor implements JudgeEnvironmentCommandExecutor {
        private final List<CommandResult> results = new ArrayList<>();
        private final List<List<String>> commands = new ArrayList<>();
        private final List<Duration> timeouts = new ArrayList<>();

        void enqueue(CommandResult result) {
            results.add(result);
        }

        @Override
        public CommandResult execute(List<String> command, Duration timeout) {
            commands.add(command);
            timeouts.add(timeout);
            return results.remove(0);
        }
    }
}
