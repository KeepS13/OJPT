package com.example.ojpt.service;

import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemTestCase;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.judge.CodeExecutionResult;
import com.example.ojpt.judge.CodeExecutionService;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.SubmissionCaseResultMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.SubmissionServiceImpl;
import com.example.ojpt.vo.SubmissionCreateResultVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionCreationServiceTest {

    @Test
    void createSubmission_returnsAcAndMarksProblemSolved() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper progressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        SubmissionCaseResultMapper caseResultMapper = mock(SubmissionCaseResultMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                progressMapper,
                judgeCaseMapper,
                caseResultMapper,
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
                        .setCaseType("SAMPLE")
                        .setSortOrder(1)
                        .setInputText("4\n2 7 11 15\n9\n")
                        .setExpectedOutput("0 1"),
                new ProblemTestCase()
                        .setProblemId(2100000000000000001L)
                        .setCaseType("HIDDEN")
                        .setSortOrder(2)
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
        when(submissionMapper.selectList(any())).thenReturn(List.of(
                new Submission().setProblemId(2100000000000000001L).setStatus("AC").setTimeMs(12),
                new Submission().setProblemId(2100000000000000001L).setStatus("AC").setTimeMs(36),
                new Submission().setProblemId(2100000000000000001L).setStatus("AC").setTimeMs(140)
        ));
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
        assertEquals(12, result.getTimeMs());
        assertEquals(1, result.getRank());
        assertEquals(2, result.getTotalCaseCount());
        assertEquals(3, result.getRankStats().getAcceptedCount());
        assertEquals("12-33 ms", result.getRankStats().getTimeBuckets().get(0).getLabel());
        assertEquals(1, result.getRankStats().getTimeBuckets().get(0).getCount());
        assertEquals("34-55 ms", result.getRankStats().getTimeBuckets().get(1).getLabel());
        assertEquals(1, result.getRankStats().getTimeBuckets().get(1).getCount());
        assertEquals("122-143 ms", result.getRankStats().getTimeBuckets().get(5).getLabel());
        assertEquals(1, result.getRankStats().getTimeBuckets().get(5).getCount());
        assertEquals(2, result.getCaseResults().size());
        assertEquals("SAMPLE", result.getCaseResults().get(0).getCaseType());
        assertEquals("HIDDEN", result.getCaseResults().get(1).getCaseType());
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
        SubmissionCaseResultMapper caseResultMapper = mock(SubmissionCaseResultMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                progressMapper,
                judgeCaseMapper,
                caseResultMapper,
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
        assertEquals(9, result.getTimeMs());
        assertEquals(null, result.getRank());
        assertEquals(1, result.getCaseResults().size());
        assertEquals("WA", result.getCaseResults().get(0).getStatus());
        assertEquals("HIDDEN", result.getCaseResults().get(0).getCaseType());
        assertNull(result.getCaseResults().get(0).getInputText());
        assertNull(result.getCaseResults().get(0).getExpectedOutput());
        assertNull(result.getCaseResults().get(0).getActualOutput());
        assertNull(result.getCaseResults().get(0).getErrorOutput());
        assertEquals("ATTEMPTED", progress.getStatus());
        assertEquals(2300000000000000010L, progress.getLastSubmissionId());
        verify(progressMapper).updateById(progress);
    }

    @Test
    void createSubmission_stopsAfterFirstFailedCase() {
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        UserProblemProgressMapper progressMapper = mock(UserProblemProgressMapper.class);
        ProblemTestCaseMapper judgeCaseMapper = mock(ProblemTestCaseMapper.class);
        SubmissionCaseResultMapper caseResultMapper = mock(SubmissionCaseResultMapper.class);
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                submissionMapper,
                problemMapper,
                progressMapper,
                judgeCaseMapper,
                caseResultMapper,
                codeExecutionService
        );

        Problem problem = new Problem()
                .setId(2100000000000000002L)
                .setProblemNo(2)
                .setStatus("PUBLISHED")
                .setSubmitCount(0L)
                .setAcceptedCount(0L)
                .setTimeLimitMs(1000)
                .setMemoryLimitKb(256000);
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(progressMapper.selectOne(any())).thenReturn(null);
        when(judgeCaseMapper.selectList(any())).thenReturn(List.of(
                new ProblemTestCase()
                        .setProblemId(2100000000000000002L)
                        .setCaseType("SAMPLE")
                        .setSortOrder(1)
                        .setInputText("abcabcbb")
                        .setExpectedOutput("3"),
                new ProblemTestCase()
                        .setProblemId(2100000000000000002L)
                        .setCaseType("SAMPLE")
                        .setSortOrder(2)
                        .setInputText("bbbbb")
                        .setExpectedOutput("1"),
                new ProblemTestCase()
                        .setProblemId(2100000000000000002L)
                        .setCaseType("HIDDEN")
                        .setSortOrder(3)
                        .setInputText("pwwkew")
                        .setExpectedOutput("3")
        ));
        when(codeExecutionService.execute(any(), any(), any(), any(), any())).thenReturn(
                CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("")
                        .stderr("")
                        .timeMs(7L)
                        .build()
        );
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            if (submission.getId() == null) {
                submission.setId(2300000000000000011L);
            }
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));

        SubmissionCreateDTO dto = new SubmissionCreateDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print('')");

        SubmissionCreateResultVO result = service.createSubmission(1001L, 2, dto);

        assertEquals("WA", result.getStatus());
        assertEquals(3, result.getTotalCaseCount());
        assertEquals(1, result.getCaseResults().size());
        assertEquals("SAMPLE", result.getCaseResults().get(0).getCaseType());
        assertEquals("abcabcbb", result.getCaseResults().get(0).getInputText());
        verify(codeExecutionService, times(1)).execute(any(), any(), any(), any(), any());
        verify(caseResultMapper, times(1)).insert(any());
    }
}
