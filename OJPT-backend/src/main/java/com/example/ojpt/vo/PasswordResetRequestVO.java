package com.example.ojpt.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class PasswordResetRequestVO {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String accountIdentifier;
    private String status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
