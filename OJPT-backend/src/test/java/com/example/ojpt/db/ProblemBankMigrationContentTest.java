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
            Path.of("src/main/resources/db/migration/V1_5__seed_sample_problem_set.sql");

    @Test
    void sampleProblemMigration_shouldContainThirtySequentialProblems() throws IOException {
        String sql = Files.readString(MIGRATION);
        String problemInsertBlock = extractProblemInsertBlock(sql);

        Matcher matcher = Pattern.compile("\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*'").matcher(problemInsertBlock);
        List<Integer> problemNos = new ArrayList<>();
        while (matcher.find()) {
            problemNos.add(Integer.parseInt(matcher.group(2)));
        }

        assertEquals(30, problemNos.size(), "示例题数量应为 30");
        for (int i = 1; i <= 30; i++) {
            assertEquals(i, problemNos.get(i - 1), "题号必须从 1 连续到 30");
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

        assertTrue(matcher.find(), "必须包含题号 2 的题面数据");

        String statement = matcher.group(1);
        assertNotNull(statement);
        assertTrue(statement.length() >= 300, "题目 2 的题面不能过于简陋");
        assertTrue(statement.contains("输入格式"), "题目 2 需要包含输入格式");
        assertTrue(statement.contains("输出格式"), "题目 2 需要包含输出格式");
        assertTrue(statement.contains("样例"), "题目 2 需要包含样例");
        assertTrue(statement.contains("数据范围"), "题目 2 需要包含数据范围");
    }

    private String extractProblemInsertBlock(String sql) {
        Matcher matcher = Pattern.compile(
                "INSERT INTO `problem`\\s*\\((.*?)\\)\\s*VALUES\\s*(.*?)ON DUPLICATE KEY UPDATE",
                Pattern.DOTALL
        ).matcher(sql);

        assertTrue(matcher.find(), "必须包含 problem 表的批量插入语句");
        return matcher.group(2);
    }
}
