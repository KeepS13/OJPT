package com.example.ojpt.judge;

import com.example.ojpt.dto.JudgeEnvironmentCheckDTO;
import com.example.ojpt.dto.JudgeEnvironmentHealthDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class JudgeEnvironmentHealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String SKIPPED = "SKIPPED";

    private final JudgeEnvironmentCommandExecutor commandExecutor;
    private final String dockerExecutable;
    private final String cppImage;
    private final String javaImage;
    private final String pythonImage;
    private final Duration timeout;

    public JudgeEnvironmentHealthService(
            JudgeEnvironmentCommandExecutor commandExecutor,
            @Value("${ojpt.judge.docker.executable}") String dockerExecutable,
            @Value("${ojpt.judge.cpp.image}") String cppImage,
            @Value("${ojpt.judge.java.image}") String javaImage,
            @Value("${ojpt.judge.python.image}") String pythonImage,
            @Value("${ojpt.judge.health.timeout-ms:3000}") Long timeoutMs) {
        this.commandExecutor = commandExecutor;
        this.dockerExecutable = dockerExecutable;
        this.cppImage = cppImage;
        this.javaImage = javaImage;
        this.pythonImage = pythonImage;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public JudgeEnvironmentHealthDTO checkHealth() {
        List<JudgeEnvironmentCheckDTO> checks = new ArrayList<>();
        DockerExecutableStatus executableStatus = checkDockerExecutable();
        checks.add(new JudgeEnvironmentCheckDTO(
                "docker-executable",
                executableStatus.available() ? UP : DOWN,
                dockerExecutable,
                executableStatus.message()
        ));

        if (!executableStatus.available()) {
            checks.add(skipped("docker-version", dockerExecutable));
            checks.add(skipped("docker-info", dockerExecutable));
            checks.add(skipped("image-cpp", "cpp"));
            checks.add(skipped("image-java", "java"));
            checks.add(skipped("image-python", "python"));
            return summarize(checks);
        }

        checks.add(runCheck("docker-version", dockerExecutable, List.of(dockerExecutable, "version"), "Command completed successfully"));
        checks.add(runCheck("docker-info", dockerExecutable, List.of(dockerExecutable, "info"), "Command completed successfully"));
        checks.add(runImageCheck("cpp", cppImage));
        checks.add(runImageCheck("java", javaImage));
        checks.add(runImageCheck("python", pythonImage));
        return summarize(checks);
    }

    private DockerExecutableStatus checkDockerExecutable() {
        try {
            Path configuredPath = Path.of(dockerExecutable);
            if ((configuredPath.isAbsolute() || dockerExecutable.contains("/") || dockerExecutable.contains("\\"))
                    && Files.isRegularFile(configuredPath)) {
                return new DockerExecutableStatus(true, "Docker executable exists");
            }
        } catch (RuntimeException ignored) {
            return new DockerExecutableStatus(false, "Docker executable path is invalid");
        }

        if (!dockerExecutable.contains("/") && !dockerExecutable.contains("\\") && findOnPath(dockerExecutable)) {
            return new DockerExecutableStatus(true, "Docker executable found on PATH");
        }

        return new DockerExecutableStatus(false, "Docker executable does not exist: " + dockerExecutable);
    }

    private boolean findOnPath(String executable) {
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return false;
        }
        List<String> names = executableNames(executable);
        for (String directory : path.split(java.io.File.pathSeparator)) {
            for (String name : names) {
                if (Files.isRegularFile(Path.of(directory, name))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> executableNames(String executable) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return List.of(executable);
        }
        String lower = executable.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".exe") || lower.endsWith(".cmd") || lower.endsWith(".bat")) {
            return List.of(executable);
        }
        return List.of(executable, executable + ".exe", executable + ".cmd", executable + ".bat");
    }

    private JudgeEnvironmentCheckDTO runImageCheck(String language, String image) {
        return runCheck(
                "image-" + language,
                language,
                List.of(dockerExecutable, "image", "inspect", image),
                "Image " + image + " is available"
        );
    }

    private JudgeEnvironmentCheckDTO runCheck(String name, String target, List<String> command, String successMessage) {
        CommandResult result = commandExecutor.execute(command, timeout);
        if (result.timedOut()) {
            return new JudgeEnvironmentCheckDTO(name, DOWN, target, "Command timed out after " + timeout.toMillis() + " ms");
        }
        if (result.exitCode() == 0) {
            return new JudgeEnvironmentCheckDTO(name, UP, target, successMessage);
        }
        return new JudgeEnvironmentCheckDTO(name, DOWN, target, commandFailureMessage(result));
    }

    private String commandFailureMessage(CommandResult result) {
        String detail = firstNonBlank(result.stderr(), result.stdout());
        if (detail.isBlank()) {
            return "Command failed with exit code " + result.exitCode();
        }
        return "Command failed with exit code " + result.exitCode() + ": " + detail;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "";
    }

    private JudgeEnvironmentCheckDTO skipped(String name, String target) {
        return new JudgeEnvironmentCheckDTO(name, SKIPPED, target, "Skipped because Docker executable is unavailable");
    }

    private JudgeEnvironmentHealthDTO summarize(List<JudgeEnvironmentCheckDTO> checks) {
        boolean healthy = checks.stream().allMatch(check -> UP.equals(check.status()));
        return new JudgeEnvironmentHealthDTO(
                healthy ? UP : DOWN,
                healthy ? "Judge Docker environment is healthy" : "Some judge Docker environment checks failed",
                checks
        );
    }

    private record DockerExecutableStatus(boolean available, String message) {
    }
}
