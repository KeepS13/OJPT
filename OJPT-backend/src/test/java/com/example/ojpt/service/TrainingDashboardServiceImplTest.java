package com.example.ojpt.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.Submission;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.SubmissionMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.TrainingDashboardServiceImpl;
import com.example.ojpt.vo.training.dashboard.TrainingDashboardRecentSubmissionVO;
import com.example.ojpt.vo.training.dashboard.UserTrainingDashboardVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingDashboardServiceImplTest {

    @Test
    void getTrainingDashboard_aggregatesUserTrainingOverview() {
        initTableInfo();

        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        UserProblemProgressMapper userProblemProgressMapper = mock(UserProblemProgressMapper.class);
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        TrainingDashboardService service = new TrainingDashboardServiceImpl(
                submissionMapper,
                userProblemProgressMapper,
                problemMapper
        );

        when(submissionMapper.selectObjs(any())).thenReturn(List.of("AC", "WA", "AC", "TLE"));
        when(submissionMapper.selectList(any())).thenReturn(List.of(
                new Submission()
                        .setId(9002L)
                        .setProblemId(2002L)
                        .setLanguage("Java")
                        .setStatus("WA")
                        .setTimeMs(15)
                        .setMemoryKb(256)
                        .setCreatedAt(LocalDateTime.of(2026, 4, 29, 11, 45)),
                new Submission()
                        .setId(9001L)
                        .setProblemId(2001L)
                        .setLanguage("Python")
                        .setStatus("AC")
                        .setTimeMs(8)
                        .setMemoryKb(192)
                        .setCreatedAt(LocalDateTime.of(2026, 4, 29, 11, 30))
        ));
        when(userProblemProgressMapper.selectList(any())).thenReturn(List.of(
                new UserProblemProgress()
                        .setUserId(1001L)
                        .setProblemId(2001L)
                        .setStatus("SOLVED"),
                new UserProblemProgress()
                        .setUserId(1001L)
                        .setProblemId(2002L)
                        .setStatus("SOLVED")
        ));
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(
                new Problem()
                        .setId(2001L)
                        .setProblemNo(1)
                        .setTitle("Two Sum")
                        .setDifficulty("EASY"),
                new Problem()
                        .setId(2002L)
                        .setProblemNo(2)
                        .setTitle("Binary Search")
                        .setDifficulty("MEDIUM")
        ));

        UserTrainingDashboardVO dashboard = service.getTrainingDashboard(1001L);

        assertEquals(4L, dashboard.getTotalSubmissions());
        assertEquals(2L, dashboard.getAcceptedSubmissions());
        assertEquals(2L, dashboard.getSolvedProblemCount());
        assertEquals(50.0, dashboard.getAcceptanceRate());
        assertEquals(Map.of("AC", 2L, "WA", 1L, "TLE", 1L), dashboard.getStatusDistribution());
        assertEquals(Map.of("EASY", 1L, "MEDIUM", 1L), dashboard.getDifficultyDistribution());
        assertEquals(2, dashboard.getRecentSubmissions().size());

        TrainingDashboardRecentSubmissionVO latest = dashboard.getRecentSubmissions().get(0);
        assertEquals(9002L, latest.getSubmissionId());
        assertEquals(2002L, latest.getProblemId());
        assertEquals(2, latest.getProblemNo());
        assertEquals("Binary Search", latest.getProblemTitle());
        assertEquals("WA", latest.getStatus());
    }

    private void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Submission.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserProblemProgress.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Problem.class);
    }
}
