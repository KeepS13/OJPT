package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.CodeRunDTO;
import com.example.ojpt.dto.SubmissionCreateDTO;
import com.example.ojpt.vo.CodeRunResultVO;
import com.example.ojpt.vo.SubmissionCreateResultVO;
import com.example.ojpt.vo.UserSubmissionRecordVO;

public interface SubmissionService {
    SubmissionCreateResultVO createSubmission(Long userId, Integer problemNo, SubmissionCreateDTO dto);

    CodeRunResultVO runCode(CodeRunDTO dto);

    PageResult<UserSubmissionRecordVO> getCurrentUserSubmissions(Long userId, Integer page, Integer size);
}
