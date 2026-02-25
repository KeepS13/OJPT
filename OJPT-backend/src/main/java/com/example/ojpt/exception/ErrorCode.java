package com.example.ojpt.exception;

import lombok.Getter;

/**
 * 业务错误码枚举
 * 
 * 错误码规则：
 * - 400xx: 请求参数错误
 * - 401xx: 认证错误
 * - 403xx: 权限错误
 * - 404xx: 资源不存在
 * - 409xx: 资源冲突
 * - 500xx: 服务器内部错误
 */
@Getter
public enum ErrorCode {
    
    // ========== 通用错误 ==========
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // ========== 参数校验错误 40001-40099 ==========
    PARAM_MISSING(40001, "缺少必要参数"),
    PARAM_INVALID(40002, "参数格式不正确"),
    PARAM_OUT_OF_RANGE(40003, "参数超出有效范围"),
    
    // ========== 认证相关 40101-40199 ==========
    TOKEN_EXPIRED(40101, "Token已过期"),
    TOKEN_INVALID(40102, "Token无效"),
    TOKEN_MISSING(40103, "未提供Token"),
    REFRESH_TOKEN_EXPIRED(40104, "Refresh Token已过期"),
    REFRESH_TOKEN_INVALID(40105, "Refresh Token无效"),
    PASSWORD_INCORRECT(40106, "密码错误"),
    ACCOUNT_DISABLED(40107, "账号已禁用"),
    ACCOUNT_PENDING(40108, "账号待审核"),
    
    // ========== 权限相关 40301-40399 ==========
    PERMISSION_DENIED(40301, "权限不足"),
    ROLE_NOT_ALLOWED(40302, "角色权限不足"),
    USER_BANNED(40303, "用户已被封禁"),
    
    // ========== 用户相关 40401-40499 ==========
    USER_NOT_FOUND(40401, "用户不存在"),
    ROLE_NOT_FOUND(40402, "角色不存在"),
    PERMISSION_NOT_FOUND(40403, "权限不存在"),
    SCHOOL_NOT_FOUND(40404, "学校不存在"),
    DEPARTMENT_NOT_FOUND(40405, "院系不存在"),
    CLASS_NOT_FOUND(40406, "班级不存在"),
    PROFILE_NOT_FOUND(40407, "用户档案不存在"),
    
    // ========== 资源冲突 40901-40999 ==========
    USERNAME_EXISTS(40901, "用户名已存在"),
    EMAIL_EXISTS(40902, "邮箱已被使用"),
    PHONE_EXISTS(40903, "手机号已被使用"),
    ROLE_CODE_EXISTS(40904, "角色编码已存在"),
    PERMISSION_EXISTS(40905, "权限已存在"),
    USER_ROLE_EXISTS(40906, "用户角色关系已存在"),
    CLASS_USER_EXISTS(40907, "用户已加入该班级"),
    SCHOOL_NAME_EXISTS(40908, "学校名称已存在"),
    
    // ========== 业务逻辑错误 50001-50099 ==========
    ROLE_HAS_USERS(50001, "该角色下还有用户，无法删除"),
    PERMISSION_IN_USE(50002, "该权限已被角色使用，无法删除"),
    DEPARTMENT_HAS_CLASSES(50003, "该院系下还有班级，无法删除"),
    SCHOOL_HAS_DEPARTMENTS(50004, "该学校下还有院系，无法删除"),
    CANNOT_DELETE_SELF(50005, "不能删除自己的账号"),
    CANNOT_MODIFY_ADMIN(50006, "不能修改超级管理员"),
    PASSWORD_SAME_AS_OLD(50007, "新密码不能与原密码相同"),
    PASSWORD_TOO_WEAK(50008, "密码强度不足"),
    
    // ========== 文件相关 50101-50199 ==========
    FILE_NOT_FOUND(50101, "文件不存在"),
    FILE_TOO_LARGE(50102, "文件过大"),
    FILE_TYPE_NOT_ALLOWED(50103, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(50104, "文件上传失败");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}
