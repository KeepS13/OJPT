package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称不能超过 30 个字符")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @Past(message = "生日必须早于今天")
    private LocalDate birthday;
}
