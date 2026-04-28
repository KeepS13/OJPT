package com.example.ojpt.service;

import com.example.ojpt.dto.CodeRunCaseDTO;
import com.example.ojpt.dto.CodeRunDTO;
import com.example.ojpt.judge.CodeExecutionResult;
import com.example.ojpt.judge.CodeExecutionService;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.SubmissionCaseResultMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.SubmissionServiceImpl;
import com.example.ojpt.vo.CodeRunResultVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionRunCodeServiceTest {

    @Test
    void runCode_executesProvidedCasesAndReturnsCaseResults() {
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
                mock(SubmissionCaseResultMapper.class),
                codeExecutionService
        );

        when(codeExecutionService.execute(eq("Python3"), eq("print(input())"), eq("abc"), eq(1000), eq(256000)))
                .thenReturn(CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("abc\n")
                        .stderr("")
                        .timeMs(10L)
                        .build());
        when(codeExecutionService.execute(eq("Python3"), eq("print(input())"), eq("def"), eq(1000), eq(256000)))
                .thenReturn(CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("xyz\n")
                        .stderr("")
                        .timeMs(11L)
                        .build());

        CodeRunDTO dto = new CodeRunDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print(input())");
        dto.setTimeLimitMs(1000);
        dto.setMemoryLimitKb(256000);
        dto.setCases(List.of(
                new CodeRunCaseDTO("abc", "abc"),
                new CodeRunCaseDTO("def", "def")
        ));

        CodeRunResultVO result = service.runCode(dto);

        assertEquals("FINISHED", result.getStatus());
        assertEquals(2, result.getCaseResults().size());
        assertEquals("AC", result.getCaseResults().get(0).getStatus());
        assertEquals("abc\n", result.getCaseResults().get(0).getActualOutput());
        assertEquals("WA", result.getCaseResults().get(1).getStatus());
        assertEquals("xyz\n", result.getCaseResults().get(1).getActualOutput());
    }

    @Test
    void runCode_stopsAfterFirstFailedCase() {
        CodeExecutionService codeExecutionService = mock(CodeExecutionService.class);
        SubmissionService service = new SubmissionServiceImpl(
                mock(SubmissionMapper.class),
                mock(ProblemMapper.class),
                mock(UserProblemProgressMapper.class),
                mock(ProblemTestCaseMapper.class),
                mock(SubmissionCaseResultMapper.class),
                codeExecutionService
        );

        when(codeExecutionService.execute(eq("Python3"), eq("print('wrong')"), eq("abc"), eq(1000), eq(256000)))
                .thenReturn(CodeExecutionResult.builder()
                        .compileSuccess(true)
                        .runtimeSuccess(true)
                        .timedOut(false)
                        .stdout("wrong\n")
                        .stderr("")
                        .timeMs(10L)
                        .build());

        CodeRunDTO dto = new CodeRunDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print('wrong')");
        dto.setTimeLimitMs(1000);
        dto.setMemoryLimitKb(256000);
        dto.setCases(List.of(
                new CodeRunCaseDTO("abc", "abc"),
                new CodeRunCaseDTO("def", "def")
        ));

        CodeRunResultVO result = service.runCode(dto);

        assertEquals(1, result.getCaseResults().size());
        assertEquals("WA", result.getCaseResults().get(0).getStatus());
        verify(codeExecutionService, times(1)).execute(any(), any(), any(), any(), any());
    }

    @Test
    void runCode_rejectsEmptyCases() {
        SubmissionService service = new SubmissionServiceImpl(
                mock(SubmissionMapper.class),
                mock(ProblemMapper.class),
                mock(UserProblemProgressMapper.class),
                mock(ProblemTestCaseMapper.class),
                mock(SubmissionCaseResultMapper.class),
                mock(CodeExecutionService.class)
        );

        CodeRunDTO dto = new CodeRunDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print(1)");
        dto.setCases(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.ojpt.exception.BusinessException.class,
                () -> service.runCode(dto)
        );
    }
}
