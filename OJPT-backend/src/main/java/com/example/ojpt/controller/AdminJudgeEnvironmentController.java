package com.example.ojpt.controller;

import com.example.ojpt.common.Result;
import com.example.ojpt.dto.JudgeEnvironmentHealthDTO;
import com.example.ojpt.judge.JudgeEnvironmentHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judge-environment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "判题环境", description = "判题 Docker 环境健康检查接口")
public class AdminJudgeEnvironmentController {

    private final JudgeEnvironmentHealthService judgeEnvironmentHealthService;

    @GetMapping("/health")
    @Operation(summary = "获取判题 Docker 环境健康状态")
    public Result<JudgeEnvironmentHealthDTO> getHealth() {
        return Result.ok(judgeEnvironmentHealthService.checkHealth());
    }
}
