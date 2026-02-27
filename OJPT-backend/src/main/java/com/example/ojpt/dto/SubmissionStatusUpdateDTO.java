package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员修改提交状态 DTO（stub 阶段用于演示 AC/WA 等状态切换）。
 */
@Data
public class SubmissionStatusUpdateDTO {

    @NotBlank(message = "状态不能为空")
    private String status;

    private Integer timeMs;

    private Integer memoryKb;

    private String compileMessage;

    private String judgeMessage;
}

