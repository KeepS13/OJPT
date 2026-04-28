package com.example.ojpt.service;

import com.example.ojpt.dto.ProblemTestCaseBatchUpdateDTO;
import com.example.ojpt.vo.ProblemTestCaseVO;

import java.util.List;

public interface ProblemTestCaseService {

    List<ProblemTestCaseVO> getSampleTestCasesByProblemNo(Integer problemNo);

    List<ProblemTestCaseVO> getProblemTestCases(Long problemId);

    void replaceProblemTestCases(Long problemId, ProblemTestCaseBatchUpdateDTO dto);
}
