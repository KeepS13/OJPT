package com.example.ojpt.dto;

public record JudgeEnvironmentCheckDTO(
        String name,
        String status,
        String target,
        String message
) {
}
