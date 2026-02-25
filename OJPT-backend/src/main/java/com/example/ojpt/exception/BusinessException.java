package com.example.ojpt.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 
 * 用于所有可预见的业务逻辑异常，会被全局异常处理器捕获并返回友好的错误响应
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final int code;
    
    /**
     * 错误消息
     */
    private final String message;
    
    /**
     * 额外数据（可选，用于返回封禁剩余时间等额外信息）
     */
    private Object data;
    
    /**
     * 使用错误码枚举构造
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    /**
     * 使用错误码枚举构造，自定义消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }
    
    /**
     * 使用错误码枚举构造，带额外数据
     */
    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.data = data;
    }
    
    /**
     * 使用错误码枚举构造，自定义消息和额外数据
     */
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.data = data;
    }
    
    /**
     * 直接指定错误码和消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 直接指定错误码、消息和额外数据
     */
    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // ========== 便捷静态工厂方法 ==========
    
    public static BusinessException notFound(String resource) {
        return new BusinessException(ErrorCode.NOT_FOUND, resource + "不存在");
    }
    
    public static BusinessException userNotFound() {
        return new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    
    public static BusinessException roleNotFound() {
        return new BusinessException(ErrorCode.ROLE_NOT_FOUND);
    }
    
    public static BusinessException roleNotFound(String roleCode) {
        return new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在: " + roleCode);
    }
    
    public static BusinessException permissionNotFound() {
        return new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
    }
    
    public static BusinessException schoolNotFound() {
        return new BusinessException(ErrorCode.SCHOOL_NOT_FOUND);
    }
    
    public static BusinessException departmentNotFound() {
        return new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
    }
    
    public static BusinessException classNotFound() {
        return new BusinessException(ErrorCode.CLASS_NOT_FOUND);
    }
    
    public static BusinessException usernameExists() {
        return new BusinessException(ErrorCode.USERNAME_EXISTS);
    }
    
    public static BusinessException emailExists() {
        return new BusinessException(ErrorCode.EMAIL_EXISTS);
    }
    
    public static BusinessException phoneExists() {
        return new BusinessException(ErrorCode.PHONE_EXISTS);
    }
    
    public static BusinessException roleCodeExists() {
        return new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
    }
    
    public static BusinessException permissionExists() {
        return new BusinessException(ErrorCode.PERMISSION_EXISTS);
    }
    
    public static BusinessException forbidden() {
        return new BusinessException(ErrorCode.FORBIDDEN);
    }
    
    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }
    
    public static BusinessException unauthorized() {
        return new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    
    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }
    
    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
    
    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
    
    public static BusinessException userBanned(long remainingSeconds) {
        return new BusinessException(ErrorCode.USER_BANNED, "用户已被封禁", remainingSeconds);
    }
}
