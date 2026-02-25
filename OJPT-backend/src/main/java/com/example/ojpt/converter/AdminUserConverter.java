package com.example.ojpt.converter;

import com.example.ojpt.entity.User;
import com.example.ojpt.vo.AdminUserVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AdminUser 转换器。
 * 
 * 原本使用 MapStruct 生成实现类，当前为简化依赖与排查成本，改为手写实现。
 */
@Component
public class AdminUserConverter {

    /**
     * User 实体 -> AdminUserVO，附带角色编码列表（做防御性复制）。
     */
    public AdminUserVO toVo(User user, List<String> roleCodes) {
        if (user == null) {
            return null;
        }
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setRoleType(user.getRoleType());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        if (roleCodes == null || roleCodes.isEmpty()) {
            vo.setRoleCodes(List.of());
        } else {
            vo.setRoleCodes(List.copyOf(roleCodes));
        }
        return vo;
    }
}

