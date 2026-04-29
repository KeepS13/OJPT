package com.example.ojpt.judge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DockerCodeExecutionService implements CodeExecutionService {

    @Value("${ojpt.judge.docker.executable}")
    private String dockerExecutable;

    @Value("${ojpt.judge.cpp.image}")
    private String cppImage;

    @Value("${ojpt.judge.java.image}")
    private String javaImage;

    @Value("${ojpt.judge.python.image}")
    private String pythonImage;

    @Value("${ojpt.judge.cpu:1}")
    private String cpuLimit;

    @Value("${ojpt.judge.memory-mb:256}")
    private Integer memoryMb;

    @Override
    public CodeExecutionResult execute(String language, String sourceCode, String inputText, Integer timeLimitMs, Integer memoryLimitKb) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("ojpt-judge-");
            Files.writeString(workspace.resolve(getSourceFileName(language)), sourceCode, StandardCharsets.UTF_8);
            Files.writeString(workspace.resolve("input.txt"), inputText == null ? "" : inputText, StandardCharsets.UTF_8);

            if (requiresCompilation(language)) {
                ProcessOutcome compileOutcome = runDockerCommand(language, workspace, getCompileCommand(language), 10000, memoryLimitKb);
                if (compileOutcome.timedOut()) {
                    return CodeExecutionResult.builder()
                            .compileSuccess(false)
                            .runtimeSuccess(false)
                            .timedOut(true)
                            .stderr("编译超时")
                            .build();
                }
                if (compileOutcome.exitCode() != 0) {
                    return CodeExecutionResult.builder()
                            .compileSuccess(false)
                            .runtimeSuccess(false)
                            .timedOut(false)
                            .stderr(compileOutcome.stderr())
                            .build();
                }
            }

            long timeoutMs = Math.max(timeLimitMs == null ? 2000 : timeLimitMs, 2000);
            long start = System.nanoTime();
            ProcessOutcome runOutcome = runDockerCommand(language, workspace, getRunCommand(language), timeoutMs, memoryLimitKb);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            if (runOutcome.timedOut()) {
                return CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(false)
                        .timedOut(true)
                        .stderr("运行超时")
                        .timeMs(timeoutMs)
                        .build();
            }

            return CodeExecutionResult.builder()
                    .compileSuccess(true)
                    .runtimeSuccess(runOutcome.exitCode() == 0)
                    .timedOut(false)
                    .stdout(runOutcome.stdout())
                    .stderr(runOutcome.stderr())
                    .timeMs(elapsed)
                    .build();
        } catch (Exception e) {
            return CodeExecutionResult.builder()
                    .compileSuccess(false)
                    .runtimeSuccess(false)
                    .timedOut(false)
                    .stderr(e.getMessage())
                    .build();
        } finally {
            if (workspace != null) {
                try {
                    Files.walk(workspace)
                            .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                } catch (IOException ignored) {
                }
            }
        }
    }

    private boolean requiresCompilation(String language) {
        return !"Python3".equalsIgnoreCase(language);
    }

    private String getSourceFileName(String language) {
        return switch (language) {
            case "Java" -> "Main.java";
            case "Python3" -> "main.py";
            default -> "Main.cpp";
        };
    }

    private String getImage(String language) {
        return switch (language) {
            case "Java" -> javaImage;
            case "Python3" -> pythonImage;
            default -> cppImage;
        };
    }

    private String getCompileCommand(String language) {
        return switch (language) {
            case "Java" -> "javac Main.java";
            default -> "g++ Main.cpp -O2 -std=c++17 -o Main";
        };
    }

    private String getRunCommand(String language) {
        return switch (language) {
            case "Java" -> "java Main < input.txt";
            case "Python3" -> "python3 main.py < input.txt";
            default -> "./Main < input.txt";
        };
    }

    private ProcessOutcome runDockerCommand(
            String language,
            Path workspace,
            String command,
            long timeoutMs,
            Integer memoryLimitKb) throws IOException, InterruptedException {
        String containerName = "ojpt-judge-" + UUID.randomUUID();
        String mountPath = workspace.toAbsolutePath().toString().replace('\\', '/');
        List<String> args = List.of(
                dockerExecutable,
                "run",
                "--rm",
                "--name",
                containerName,
                "--network",
                "none",
                "--cpus",
                cpuLimit,
                "--memory",
                resolveMemoryLimit(memoryLimitKb),
                "-v",
                mountPath + ":/workspace",
                "-w",
                "/workspace",
                getImage(language),
                "sh",
                "-lc",
                command
        );

        Process process = new ProcessBuilder(args).start();
        boolean completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            new ProcessBuilder(dockerExecutable, "rm", "-f", containerName).start().waitFor(10, TimeUnit.SECONDS);
            return new ProcessOutcome(-1, "", "", true);
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessOutcome(process.exitValue(), stdout, stderr, false);
    }

    private String resolveMemoryLimit(Integer memoryLimitKb) {
        if (memoryLimitKb != null && memoryLimitKb > 0) {
            return String.valueOf(memoryLimitKb.longValue() * 1024L);
        }
        return memoryMb + "m";
    }

    private record ProcessOutcome(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}
