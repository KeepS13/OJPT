package com.example.ojpt.security;

import java.util.Collection;
import java.util.List;

public final class SystemRoleScope {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private SystemRoleScope() {
    }

    public static String normalizeRoleType(String roleType) {
        return isAdminRole(roleType) ? ADMIN : USER;
    }

    public static List<String> normalizeRoleCodes(Collection<String> roleCodes) {
        return List.of(resolvePrimaryRole(roleCodes));
    }

    public static String resolvePrimaryRole(Collection<String> roleCodes) {
        if (roleCodes == null) {
            return USER;
        }

        for (String roleCode : roleCodes) {
            if (isAdminRole(roleCode)) {
                return ADMIN;
            }
        }

        return USER;
    }

    public static boolean isAdminRole(String roleCode) {
        return roleCode != null && ADMIN.equalsIgnoreCase(roleCode.trim());
    }
}
