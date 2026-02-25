package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码 DTO。
 */
@Data
public class PasswordUpdateDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     * 规则：至少8个字符，包含字母和数字
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码长度至少为8个字符")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "新密码必须包含字母和数字")
    private String newPassword;
}

