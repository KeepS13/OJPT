package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    /**
     * 登录账号：邮箱或手机号。
     */
    @NotBlank
    private String account;

    @NotBlank
    private String password;
}

