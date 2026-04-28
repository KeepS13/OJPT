package com.example.ojpt.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ProblemCodeDraftVO {

    private Integer problemNo;

    private String language;

    private String sourceCode;

    private LocalDateTime updatedAt;
}
