package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.PermissionCreateDTO;
import com.example.ojpt.dto.PermissionUpdateDTO;
import com.example.ojpt.dto.RoleCreateDTO;
import com.example.ojpt.dto.RolePermissionAssignDTO;
import com.example.ojpt.dto.RoleUpdateDTO;
import com.example.ojpt.dto.SchoolCreateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.dto.UserRoleUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.vo.PermissionVO;
import com.example.ojpt.vo.RoleVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.UserDetailVO;

import java.util.List;

public interface AdminService {
    
    // 用户管理扩展
    PageResult<UserDetailVO> getUsers(Integer page, Integer size, Integer status, String roleType, String keyword);
    UserDetailVO getUser(Long userId);
    void updateUser(Long userId, UserUpdateDTO dto);
    void deleteUser(Long userId);
    void updateUserStatus(Long userId, Integer status);
    void updateUserRoles(Long userId, UserRoleUpdateDTO dto);
    
    // 角色权限管理
    List<RoleVO> getRoles();
    RoleVO createRole(RoleCreateDTO dto);
    RoleVO getRole(Long roleId);
    void updateRole(Long roleId, RoleUpdateDTO dto);
    void deleteRole(Long roleId);
    
    List<PermissionVO> getPermissions(String resource, String action, String keyword);
    PermissionVO createPermission(PermissionCreateDTO dto);
    PermissionVO getPermission(Long permissionId);
    void updatePermission(Long permissionId, PermissionUpdateDTO dto);
    void deletePermission(Long permissionId);
    
    void assignPermissionsToRole(Long roleId, RolePermissionAssignDTO dto);
    void removePermissionFromRole(Long roleId, Long permissionId);
    
    // 学校管理
    PageResult<SchoolVO> getSchools(Integer page, Integer size, Integer status, String keyword);
    SchoolVO createSchool(SchoolCreateDTO dto);
    SchoolVO getSchool(Long schoolId);
    void updateSchool(Long schoolId, SchoolUpdateDTO dto);
    void deleteSchool(Long schoolId);
    void updateSchoolStatus(Long schoolId, Integer status);
    void certifySchool(Long schoolId);
    void uncertifySchool(Long schoolId);
    
    // 数据统计
    StatisticsVO getOverviewStatistics();
    StatisticsVO getUserStatistics();
    StatisticsVO getSchoolStatistics();
}




