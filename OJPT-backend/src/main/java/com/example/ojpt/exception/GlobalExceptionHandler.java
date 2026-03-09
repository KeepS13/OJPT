package com.example.ojpt.exception;

import com.example.ojpt.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一返回 Result 格式的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 - 最高优先级
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        
        Result<Object> result = Result.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .data(ex.getData())
                .timestamp(System.currentTimeMillis())
                .build();
        
        // 根据错误码确定HTTP状态码
        HttpStatus httpStatus = mapCodeToHttpStatus(ex.getCode());
        return ResponseEntity.status(httpStatus).body(result);
    }
    
    /**
     * 处理用户封禁异常
     */
    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<Result<Object>> handleUserBannedException(UserBannedException ex) {
        log.warn("用户封禁异常: remainingSeconds={}", ex.getRemainingSeconds());
        
        Map<String, Object> data = new HashMap<>();
        data.put("remainingSeconds", ex.getRemainingSeconds());
        
        Result<Object> result = Result.builder()
                .code(ErrorCode.USER_BANNED.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : "账号已被封禁")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * 处理登录凭证错误（用户名或密码错误）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        String message = ex.getMessage();
        // Spring Security 默认会给出 "Bad credentials"，这里统一为中文提示，避免前端展示英文
        if (message == null || message.isBlank() || "Bad credentials".equalsIgnoreCase(message)) {
            message = "用户名或密码错误";
        }
        log.warn("登录凭证错误: {}", message);
        Result<Void> result = Result.error(ErrorCode.PASSWORD_INCORRECT.getCode(), message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * 处理账号不可用（已禁用等）
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Result<Void>> handleDisabledException(DisabledException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "账号不可用";
        }
        log.warn("账号不可用: {}", message);
        Result<Void> result = Result.error(ErrorCode.ACCOUNT_DISABLED.getCode(), message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * 处理 ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        
        Result<Void> result = Result.error(ex.getStatusCode().value(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(result);
    }
    
    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "参数错误",
                        (a, b) -> a
                ));
        
        String message = errors.values().stream().findFirst().orElse("参数校验失败");
        log.warn("参数校验失败: {}", errors);
        
        Result<Map<String, String>> result = Result.<Map<String, String>>builder()
                .code(ErrorCode.PARAM_INVALID.getCode())
                .message(message)
                .data(errors)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        
        Result<Void> result = Result.badRequest(ex.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理其他 RuntimeException - 兜底
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("未预期的运行时异常", ex);
        
        // 根据异常消息判断HTTP状态码（兼容旧代码）
        String message = ex.getMessage();
        int code;
        HttpStatus httpStatus;
        
        if (message != null) {
            if (message.contains("不存在") || message.contains("未找到")) {
                code = ErrorCode.NOT_FOUND.getCode();
                httpStatus = HttpStatus.NOT_FOUND;
            } else if (message.contains("无权") || message.contains("权限")) {
                code = ErrorCode.FORBIDDEN.getCode();
                httpStatus = HttpStatus.FORBIDDEN;
            } else if (message.contains("已存在") || message.contains("冲突")) {
                code = ErrorCode.CONFLICT.getCode();
                httpStatus = HttpStatus.CONFLICT;
            } else {
                code = ErrorCode.BAD_REQUEST.getCode();
                httpStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            code = ErrorCode.INTERNAL_ERROR.getCode();
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "服务器内部错误";
        }
        
        Result<Void> result = Result.error(code, message);
        return ResponseEntity.status(httpStatus).body(result);
    }
    
    /**
     * 处理所有其他异常 - 最终兜底
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("未预期的异常", ex);
        
        Result<Void> result = Result.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                "服务器内部错误"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
    
    /**
     * 将业务错误码映射到HTTP状态码
     */
    private HttpStatus mapCodeToHttpStatus(int code) {
        if (code >= 40001 && code < 40100) {
            return HttpStatus.BAD_REQUEST;
        } else if (code >= 40101 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        } else if (code >= 40301 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        } else if (code >= 40401 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        } else if (code >= 40901 && code < 41000) {
            return HttpStatus.CONFLICT;
        } else if (code >= 50001) {
            return HttpStatus.BAD_REQUEST; // 业务逻辑错误返回400
        } else if (code == 200) {
            return HttpStatus.OK;
        } else if (code == 400) {
            return HttpStatus.BAD_REQUEST;
        } else if (code == 401) {
            return HttpStatus.UNAUTHORIZED;
        } else if (code == 403) {
            return HttpStatus.FORBIDDEN;
        } else if (code == 404) {
            return HttpStatus.NOT_FOUND;
        } else if (code == 409) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

