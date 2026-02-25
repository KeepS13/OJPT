package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改手机号 DTO。
 */
@Data
public class PhoneUpdateDTO {

    /**
     * 新手机号
     * 规则：1开头的11位数字
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确，必须是1开头的11位数字")
    private String phone;
}

