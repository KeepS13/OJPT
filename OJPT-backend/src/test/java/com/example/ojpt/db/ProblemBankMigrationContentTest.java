package com.example.ojpt.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemBankMigrationContentTest {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V1_0__baseline_schema_and_seed.sql");

    @Test
    void sampleProblemMigration_shouldContainThirtySequentialProblems() throws IOException {
        String sql = Files.readString(MIGRATION);
        String problemInsertBlock = extractProblemInsertBlock(sql);

        Matcher matcher = Pattern.compile("\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*'").matcher(problemInsertBlock);
        List<Integer> problemNos = new ArrayList<>();
        while (matcher.find()) {
            problemNos.add(Integer.parseInt(matcher.group(2)));
        }

        assertEquals(30, problemNos.size(), "sample problem count should be 30");
        for (int i = 1; i <= 30; i++) {
            assertEquals(i, problemNos.get(i - 1), "problem numbers should be continuous from 1 to 30");
        }
    }

    @Test
    void sampleProblemMigration_shouldProvideRichProblemTwoStatement() throws IOException {
        String sql = Files.readString(MIGRATION);
        String problemInsertBlock = extractProblemInsertBlock(sql);

        Matcher matcher = Pattern.compile(
                "\\(\\s*\\d+\\s*,\\s*2\\s*,\\s*'[^']*'\\s*,\\s*'[^']*'\\s*,\\s*'([^']*)'",
                Pattern.DOTALL
        ).matcher(problemInsertBlock);

        assertTrue(matcher.find(), "problem 2 statement data should exist");

        String statement = matcher.group(1);
        assertNotNull(statement);
        assertTrue(statement.length() >= 300, "problem 2 statement should be detailed enough");
        assertTrue(statement.contains("输入格式"), "problem 2 should include input format");
        assertTrue(statement.contains("输出格式"), "problem 2 should include output format");
        assertTrue(statement.contains("样例"), "problem 2 should include examples");
        assertTrue(statement.contains("数据范围"), "problem 2 should include constraints");
    }

    @Test
    void baselineMigration_shouldUseCleanDefaultAccountSeeds() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(!sql.contains("-- Source: V1_"), "baseline should not retain merged source migration markers");
        assertTrue(!sql.contains("/avatars/"), "default accounts should not point at missing avatar files");
        assertTrue(!Pattern.compile("(?m)^\\s*(DELETE FROM|SET @)").matcher(sql).find(),
                "fresh baseline should not use cleanup/delete patch sections");

        assertDefaultUserHasNullAvatar(sql, 1998338632572506113L, "admin", "admin@qq.com");
        assertDefaultUserHasNullAvatar(sql, 1998338632572506114L, "admin1", "admin1@qq.com");
        assertDefaultUserHasNullAvatar(sql, 1998338632572506117L, "user", "user@qq.com");
        assertDefaultUserHasNullAvatar(sql, 1998338632572506121L, "user1", "user1@qq.com");
    }

    private String extractProblemInsertBlock(String sql) {
        Matcher matcher = Pattern.compile(
                "INSERT INTO `problem`\\s*\\((.*?)\\)\\s*VALUES\\s*(.*?)ON DUPLICATE KEY UPDATE",
                Pattern.DOTALL
        ).matcher(sql);

        while (matcher.find()) {
            String problemInsertBlock = matcher.group(2);
            Matcher problemNoMatcher = Pattern.compile("\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*'")
                    .matcher(problemInsertBlock);
            int problemCount = 0;
            while (problemNoMatcher.find()) {
                problemCount++;
            }
            if (problemCount == 30) {
                return problemInsertBlock;
            }
        }

        throw new AssertionError("a problem insert block with 30 sample problems should exist");
    }

    private void assertDefaultUserHasNullAvatar(String sql, long id, String username, String email) {
        Pattern pattern = Pattern.compile(
                "\\(\\s*" + id + "\\s*,\\s*'" + username + "'\\s*,\\s*'" + email
                        + "'\\s*,\\s*'[^']*'\\s*,\\s*NULL\\s*,",
                Pattern.DOTALL
        );
        assertTrue(pattern.matcher(sql).find(), username + " should be seeded with a NULL avatar");
    }
}
