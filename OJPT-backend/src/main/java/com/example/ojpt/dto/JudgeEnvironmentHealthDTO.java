package com.example.ojpt.dto;

import java.util.List;

public record JudgeEnvironmentHealthDTO(
        String status,
        String message,
        List<JudgeEnvironmentCheckDTO> checks
) {
}
