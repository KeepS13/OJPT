package com.example.ojpt.service;

import com.example.ojpt.dto.ProblemCodeDraftSaveDTO;
import com.example.ojpt.entity.Problem;
import com.example.ojpt.entity.ProblemCodeDraft;
import com.example.ojpt.mapper.ProblemCodeDraftMapper;
import com.example.ojpt.mapper.ProblemMapper;
import com.example.ojpt.service.impl.ProblemCodeDraftServiceImpl;
import com.example.ojpt.vo.ProblemCodeDraftVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemCodeDraftServiceTest {

    @Test
    void saveDraft_insertsDraftForUserProblemAndLanguage() {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        ProblemCodeDraftMapper draftMapper = mock(ProblemCodeDraftMapper.class);
        ProblemCodeDraftService service = new ProblemCodeDraftServiceImpl(problemMapper, draftMapper);
        Problem problem = new Problem()
                .setId(2100000000000000002L)
                .setProblemNo(2)
                .setStatus("PUBLISHED");
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(draftMapper.selectOne(any())).thenReturn(null);

        ProblemCodeDraftSaveDTO dto = new ProblemCodeDraftSaveDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("print('draft')");

        ProblemCodeDraftVO result = service.saveDraft(1001L, 2, dto);

        ArgumentCaptor<ProblemCodeDraft> captor = ArgumentCaptor.forClass(ProblemCodeDraft.class);
        verify(draftMapper).insert(captor.capture());
        ProblemCodeDraft saved = captor.getValue();
        assertEquals(1001L, saved.getUserId());
        assertEquals(2100000000000000002L, saved.getProblemId());
        assertEquals("Python3", saved.getLanguage());
        assertEquals("print('draft')", saved.getSourceCode());
        assertEquals("print('draft')", result.getSourceCode());
    }

    @Test
    void saveDraft_updatesExistingDraftForSameUserProblemAndLanguage() {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        ProblemCodeDraftMapper draftMapper = mock(ProblemCodeDraftMapper.class);
        ProblemCodeDraftService service = new ProblemCodeDraftServiceImpl(problemMapper, draftMapper);
        Problem problem = new Problem()
                .setId(2100000000000000002L)
                .setProblemNo(2)
                .setStatus("PUBLISHED");
        ProblemCodeDraft existing = new ProblemCodeDraft()
                .setId(3001L)
                .setUserId(1001L)
                .setProblemId(2100000000000000002L)
                .setLanguage("Python3")
                .setSourceCode("old");
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(draftMapper.selectOne(any())).thenReturn(existing);

        ProblemCodeDraftSaveDTO dto = new ProblemCodeDraftSaveDTO();
        dto.setLanguage("Python3");
        dto.setSourceCode("new");

        ProblemCodeDraftVO result = service.saveDraft(1001L, 2, dto);

        assertEquals("new", existing.getSourceCode());
        assertEquals("new", result.getSourceCode());
        verify(draftMapper).updateById(existing);
    }

    @Test
    void getDraft_returnsNullWhenCurrentUserHasNoDraft() {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        ProblemCodeDraftMapper draftMapper = mock(ProblemCodeDraftMapper.class);
        ProblemCodeDraftService service = new ProblemCodeDraftServiceImpl(problemMapper, draftMapper);
        Problem problem = new Problem()
                .setId(2100000000000000002L)
                .setProblemNo(2)
                .setStatus("PUBLISHED");
        when(problemMapper.selectOne(any())).thenReturn(problem);
        when(draftMapper.selectOne(any())).thenReturn(null);

        assertNull(service.getDraft(1001L, 2, "Python3"));
    }
}
