package com.example.ojpt.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionAssignDTO {
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}




