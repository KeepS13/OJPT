package com.example.ojpt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProblemTestCaseBatchUpdateDTO {

    @NotNull(message = "测试用例集合不能为空")
    @Valid
    private List<ProblemTestCaseItemDTO> cases;
}
