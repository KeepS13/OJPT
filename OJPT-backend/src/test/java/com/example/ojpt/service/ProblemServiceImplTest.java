package com.example.ojpt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.UserProblemProgress;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.mapper.ProblemTagMapper;
import com.example.ojpt.mapper.ProblemTestCaseMapper;
import com.example.ojpt.mapper.TagMapper;
import com.example.ojpt.mapper.UserProblemProgressMapper;
import com.example.ojpt.service.impl.ProblemServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemServiceImplTest {

    @Test
    void queryProblems_treatsBarePKeywordAsDisplayedProblemNumberPrefix() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Problem.class);

        ProblemMapper problemMapper = mock(ProblemMapper.class);
        ProblemTestCaseMapper problemTestCaseMapper = mock(ProblemTestCaseMapper.class);
        TagMapper tagMapper = mock(TagMapper.class);
        ProblemTagMapper problemTagMapper = mock(ProblemTagMapper.class);
        UserProblemProgressMapper userProblemProgressMapper = mock(UserProblemProgressMapper.class);
        ProblemService service = new ProblemServiceImpl(
                problemMapper,
                problemTestCaseMapper,
                tagMapper,
                problemTagMapper,
                userProblemProgressMapper
        );

        Problem first = new Problem()
                .setId(2001L)
                .setProblemNo(1)
                .setTitle("涓ゆ暟涔嬪拰")
                .setDifficulty("EASY")
                .setSubmitCount(10L)
                .setAcceptedCount(8L);
        Problem second = new Problem()
                .setId(2002L)
                .setProblemNo(2)
                .setTitle("鏈€闀垮瓙涓?旋")
                .setDifficulty("MEDIUM")
                .setSubmitCount(0L)
                .setAcceptedCount(0L);

        Page<Problem> page = new Page<>(1, 20);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        page.setCurrent(1);
        page.setSize(20);
        page.setPages(1);

        Page<Problem> emptyPage = new Page<>(1, 20);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        emptyPage.setCurrent(1);
        emptyPage.setSize(20);
        emptyPage.setPages(0);

        when(problemMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Problem> wrapper = invocation.getArgument(1);
            return wrapper.getSqlSegment().contains("title LIKE") ? emptyPage : page;
        });
        when(problemTagMapper.selectList(any())).thenReturn(List.of());
        when(userProblemProgressMapper.selectList(any())).thenReturn(List.of());

        var result = service.queryProblems(1001L, 1, 20, "p", null, null, null, null);

        assertEquals(2, result.getRecords().size());
        assertEquals(List.of(2001L, 2002L), result.getRecords().stream().map(item -> item.getId()).toList());
    }

    @Test
    void queryProblems_treatsProblemsWithoutProgressAsUnsolvedWhenFilteringByStatus() {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        ProblemTestCaseMapper problemTestCaseMapper = mock(ProblemTestCaseMapper.class);
        TagMapper tagMapper = mock(TagMapper.class);
        ProblemTagMapper problemTagMapper = mock(ProblemTagMapper.class);
        UserProblemProgressMapper userProblemProgressMapper = mock(UserProblemProgressMapper.class);
        ProblemService service = new ProblemServiceImpl(
                problemMapper,
                problemTestCaseMapper,
                tagMapper,
                problemTagMapper,
                userProblemProgressMapper
        );

        Problem solved = new Problem()
                .setId(2001L)
                .setProblemNo(1)
                .setTitle("两数之和")
                .setDifficulty("EASY")
                .setSubmitCount(10L)
                .setAcceptedCount(8L);
        Problem unsolved = new Problem()
                .setId(2002L)
                .setProblemNo(2)
                .setTitle("最长子串")
                .setDifficulty("MEDIUM")
                .setSubmitCount(0L)
                .setAcceptedCount(0L);

        Page<Problem> page = new Page<>(1, 20);
        page.setRecords(List.of(solved, unsolved));
        page.setTotal(2);
        page.setCurrent(1);
        page.setSize(20);
        page.setPages(1);

        when(problemMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(problemTagMapper.selectList(any())).thenReturn(List.of());
        when(userProblemProgressMapper.selectList(any())).thenReturn(List.of(
                new UserProblemProgress()
                        .setUserId(1001L)
                        .setProblemId(2001L)
                        .setStatus("SOLVED")
        ));

        var result = service.queryProblems(1001L, 1, 20, null, null, null, "UNSOLVED", null);

        assertEquals(1, result.getRecords().size());
        assertEquals(2002L, result.getRecords().get(0).getId());
        assertEquals("UNSOLVED", result.getRecords().get(0).getStatus());
    }
}
