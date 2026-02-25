package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户名 DTO。
 */
@Data
public class UsernameUpdateDTO {

    /**
     * 新用户名
     * 规则：2-20个字符，允许中文、字母、数字、下划线、空格以及特殊字符（上标、下标、修饰字母等）
     * 示例：x²-y², x⁵⁽ⁿ⁻⁶⁾, H₂O, CO₂, H₂SO₄, H⁺, Fe²⁺, Al³, ᴴᵉˡˡᵒ, ᵂᵉˡᶜᵒᵐᵉ, Lₒᵥₑ Yₒᵤ, Cₐₗₗ Mₑ, ¹⁹⁹⁹₀₂.₁₆
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{M}_\\s.\\-\\u00B2\\u00B3\\u00B9\\u2070-\\u207F\\u2080-\\u208F\\u2090-\\u209F\\u1D00-\\u1D7F\\u1D80-\\u1DBF\\u1DC0-\\u1DFF\\u02B0-\\u02FF]+$", message = "用户名包含不允许的字符")
    private String username;
}

