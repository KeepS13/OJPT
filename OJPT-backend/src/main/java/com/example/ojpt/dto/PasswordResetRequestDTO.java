package com.example.ojpt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequestDTO {

    @NotBlank
    @Size(max = 128)
    private String account;
}
