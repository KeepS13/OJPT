package com.example.ojpt.service;

import com.example.ojpt.dto.ProblemCodeDraftSaveDTO;
import com.example.ojpt.vo.ProblemCodeDraftVO;

public interface ProblemCodeDraftService {

    ProblemCodeDraftVO getDraft(Long userId, Integer problemNo, String language);

    ProblemCodeDraftVO saveDraft(Long userId, Integer problemNo, ProblemCodeDraftSaveDTO dto);
}
