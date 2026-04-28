package com.example.ojpt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.judge.CodeExecutionService;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.SubmissionServiceImpl;
import com.example.ojpt.vo.UserSubmissionRecordVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionServiceImplTest {

    @Test
    void getCurrentUserSubmissions_returnsPagedSubmissionRecordsWithProblemInfo() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper userProblemProgressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                userProblemProgressMapper,
                judgeCaseMapper,
                codeExecutionService
        );

        Submission submission = new Submission()
                .setId(9001L)
                .setUserId(1001L)
                .setProblemId(2001L)
                .setLanguage("Java")
                .setSourceCode("public class Main {}")
                .setStatus("AC")
                .setTimeMs(12)
                .setMemoryKb(256)
                .setCreatedAt(LocalDateTime.of(2026, 4, 27, 18, 0));

        Page<Submission> page = new Page<>(1, 10);
        page.setRecords(List.of(submission));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(10);
        page.setPages(1);

        when(submissionMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(
                new Problem().setId(2001L).setProblemNo(1).setTitle("两数之和")
        ));

        var result = service.getCurrentUserSubmissions(1001L, 1, 10);

        assertEquals(1, result.getRecords().size());
        UserSubmissionRecordVO record = result.getRecords().get(0);
        assertEquals(9001L, record.getSubmissionId());
        assertEquals(2001L, record.getProblemId());
        assertEquals(1, record.getProblemNo());
        assertEquals("两数之和", record.getProblemTitle());
        assertEquals("Java", record.getLanguage());
        assertEquals("AC", record.getStatus());
        assertEquals("public class Main {}", record.getSourceCode());
        assertEquals(1, result.getTotal());
    }

    @Test
    void getCurrentUserSubmissions_returnsEmptyPageWithoutQueryingProblemsWhenNoSubmissions() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper userProblemProgressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                userProblemProgressMapper,
                judgeCaseMapper,
                codeExecutionService
        );

        Page<Submission> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        page.setCurrent(1);
        page.setSize(10);
        page.setPages(0);

        when(submissionMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.getCurrentUserSubmissions(1001L, 1, 10);

        assertEquals(0, result.getRecords().size());
        assertEquals(0, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(0, result.getPages());
        verify(problemMapper, never()).selectBatchIds(any());
    }
}
