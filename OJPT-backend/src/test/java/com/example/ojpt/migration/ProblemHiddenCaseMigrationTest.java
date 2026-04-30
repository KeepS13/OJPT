package com.example.ojpt.migration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemHiddenCaseMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @ParameterizedTest
    @ValueSource(longs = {
            2100000000000000001L,
            2100000000000000002L,
            2100000000000000003L,
            2100000000000000004L,
            2100000000000000005L,
            2100000000000000006L,
            2100000000000000007L,
            2100000000000000008L,
            2100000000000000009L,
            2100000000000000010L,
            2100000000000000011L,
            2100000000000000012L,
            2100000000000000013L,
            2100000000000000014L,
            2100000000000000015L,
            2100000000000000016L,
            2100000000000000017L,
            2100000000000000018L,
            2100000000000000019L,
            2100000000000000020L,
            2100000000000000021L,
            2100000000000000022L,
            2100000000000000023L,
            2100000000000000024L,
            2100000000000000025L,
            2100000000000000026L,
            2100000000000000027L,
            2100000000000000028L,
            2100000000000000029L,
            2100000000000000030L
    })
    void completedProblemsHaveAtLeastTenHiddenCasesInMigrations(long problemId) throws IOException {
        assertTrue(countHiddenCases(problemId) >= 10);
    }

    private long countHiddenCases(long problemId) throws IOException {
        Pattern pattern = Pattern.compile("\\(\\s*\\d+\\s*,\\s*" + problemId + "\\s*,\\s*'HIDDEN'");
        try (var files = Files.list(MIGRATION_DIR)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .mapToLong(path -> countMatches(path, pattern))
                    .sum();
        }
    }

    private long countMatches(Path path, Pattern pattern) {
        try {
            String sql = Files.readString(path, StandardCharsets.UTF_8);
            return pattern.matcher(sql).results().count();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migration: " + path, e);
        }
    }
}
