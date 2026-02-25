package com.example.ojpt.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class LoginUserDetails extends User {

    private final Long userId;
    private final Integer status;

    public LoginUserDetails(Long userId,
                            String username,
                            String password,
                            Collection<? extends GrantedAuthority> authorities,
                            boolean enabled,
                            boolean accountNonExpired,
                            boolean credentialsNonExpired,
                            boolean accountNonLocked,
                            Integer status) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getStatus() {
        return status;
    }
}

