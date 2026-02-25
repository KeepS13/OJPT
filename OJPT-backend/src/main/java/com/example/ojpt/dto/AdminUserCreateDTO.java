package com.example.ojpt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员创建用户入参 DTO。
 */
@Data
public class AdminUserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6~64位之间")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    /**
     * 角色编码列表，缺省则默认 USER。
     */
    private List<String> roleCodes;
}

