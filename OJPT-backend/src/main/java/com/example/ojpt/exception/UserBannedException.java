package com.example.ojpt.exception;

import org.springframework.security.authentication.DisabledException;

/**
 * 用户被封禁/拉黑异常，携带剩余封禁时间（秒）。
 */
public class UserBannedException extends DisabledException {

    private final long remainingSeconds;

    public UserBannedException(String msg, long remainingSeconds) {
        super(msg);
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}


