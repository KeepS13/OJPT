package com.example.ojpt.judge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerCodeExecutionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_passesProblemMemoryLimitToDockerRuntime() throws IOException {
        Path captureFile = tempDir.resolve("memory.txt");
        Path fakeDocker = tempDir.resolve("fake-docker.cmd");
        Files.writeString(fakeDocker, String.join("\r\n",
                "@echo off",
                "setlocal",
                "set \"CAPTURE=" + captureFile + "\"",
                ":loop",
                "if \"%~1\"==\"\" goto end",
                "if /I \"%~1\"==\"--memory\" (",
                "  > \"%CAPTURE%\" echo %~2",
                "  exit /b 0",
                ")",
                "shift",
                "goto loop",
                ":end",
                "> \"%CAPTURE%\" echo MISSING",
                "exit /b 1",
                ""));

        DockerCodeExecutionService service = new DockerCodeExecutionService();
        ReflectionTestUtils.setField(service, "dockerExecutable", fakeDocker.toString());
        ReflectionTestUtils.setField(service, "cppImage", "gcc:13.2.0");
        ReflectionTestUtils.setField(service, "javaImage", "eclipse-temurin:17-jdk");
        ReflectionTestUtils.setField(service, "pythonImage", "python:3.11");
        ReflectionTestUtils.setField(service, "cpuLimit", "1");
        ReflectionTestUtils.setField(service, "memoryMb", 256);

        CodeExecutionResult result = service.execute("Python3", "print(input())", "hello", 1000, 65536);

        assertTrue(result.isCompileSuccess());
        assertTrue(result.isRuntimeSuccess());
        assertEquals("67108864", Files.readString(captureFile).trim());
    }
}
