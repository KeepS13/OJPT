package com.example.ojpt.service;

import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemTestCase;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.judge.CodeExecutionResult;
import com.example.ojpt.judge.CodeExecutionService;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.SubmissionServiceImpl;
import com.example.ojpt.vo.SubmissionCreateResultVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionCreationServiceTest {

    @Test
    void createSubmission_returnsAcAndMarksProblemSolved() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper progressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                progressMapper,
                judgeCaseMapper,
                codeExecutionService
        );

        Problem problem = new Problem()
                .setId(2100000000000000001L)
                .setProblemNo(1)
                .setTitle("两数之和")
                .setStatus("PUBLISHED")
                .setSubmitCount(10L)
                .setAcceptedCount(5L)
                .setTimeLimitMs(1000)
                .setMemoryLimitKb(256000);
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(progressMapper.selectOne(any())).thenReturn(null);
        when(judgeCaseMapper.selectList(any())).thenReturn(List.of(
                new ProblemTestCase()
                        .setProblemId(2100000000000000001L)
                        .setCaseType("HIDDEN")
                        .setSortOrder(1)
                        .setInputText("4\n2 7 11 15\n9\n")
                        .setExpectedOutput("0 1")
        ));
        when(codeExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(
                CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("0 1")
                        .stderr("")
                        .timeMs(12L)
                        .build()
        );
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(2300000000000000009L);
            }
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));

        SubmissionCreateDTO dto = new SubmissionCreateDTO();
        dto.setLanguage("Java");
        dto.setSourceCode("public class Main {}");

        SubmissionCreateResultVO result = service.createSubmission(1001L, 1, dto);

        assertNotNull(result);
        assertEquals(2300000000000000009L, result.getSubmissionId());
        assertEquals("AC", result.getStatus());
        assertEquals("判题通过", result.getMessage());
        assertEquals(11L, problem.getSubmitCount());
        assertEquals(6L, problem.getAcceptedCount());
        verify(problemMapper, org.mockito.Mockito.times(2)).updateById(problem);
        verify(progressMapper).insert(any(UserProblemProgress.class));
    }

    @Test
    void createSubmission_keepsAttemptedWhenAnswerIsWrong() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper progressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                progressMapper,
                judgeCaseMapper,
                codeExecutionService
        );

        Problem problem = new Problem()
                .setId(2100000000000000001L)
                .setProblemNo(1)
                .setTitle("两数之和")
                .setStatus("PUBLISHED")
                .setSubmitCount(3L)
                .setAcceptedCount(1L)
                .setTimeLimitMs(1000)
                .setMemoryLimitKb(256000);
        UserProblemProgress progress = new UserProblemProgress()
                .setId(3001L)
                .setUserId(1001L)
                .setProblemId(2100000000000000001L)
                .setStatus("SOLVED")
                .setLastSubmissionId(88L);
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(progressMapper.selectOne(any())).thenReturn(progress);
        when(judgeCaseMapper.selectList(any())).thenReturn(List.of(
                new ProblemTestCase()
                        .setProblemId(2100000000000000001L)
                        .setCaseType("HIDDEN")
                        .setSortOrder(1)
                        .setInputText("4\n2 7 11 15\n9\n")
                        .setExpectedOutput("0 1")
        ));
        when(codeExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(
                CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("0 2")
                        .stderr("")
                        .timeMs(9L)
                        .build()
        );
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(2300000000000000010L);
            }
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));

        SubmissionCreateDTO dto = new SubmissionCreateDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print(1)");

        SubmissionCreateResultVO result = service.createSubmission(1001L, 1, dto);

        assertEquals(2300000000000000010L, result.getSubmissionId());
        assertEquals("WA", result.getStatus());
        assertEquals("答案错误", result.getMessage());
        assertEquals("ATTEMPTED", progress.getStatus());
        assertEquals(2300000000000000010L, progress.getLastSubmissionId());
        verify(progressMapper).updateById(progress);
    }
}
