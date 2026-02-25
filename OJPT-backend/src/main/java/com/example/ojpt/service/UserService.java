package com.example.ojpt.service;

import com.example.ojpt.dto.EmailUpdateDTO;
import com.example.ojpt.dto.PasswordUpdateDTO;
import com.example.ojpt.dto.PhoneUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.dto.UsernameUpdateDTO;
import com.example.ojpt.entity.User;
import com.example.ojpt.vo.CurrentUserVO;
import com.example.ojpt.vo.UserDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户查询相关服务，供控制层调用。
 */
public interface UserService {

    User findByEmail(String email);

    User findByUsername(String username);

    User findById(Long id);

    /**
     * 通过手机号查找用户（用于手机号登录）。
     */
    User findByPhone(String phone);

    /**
     * 更新用户头像路径。
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像相对路径（例如 /uploads/avatars/xxx.webp）
     */
    void updateAvatar(Long userId, String avatarUrl);

    /**
     * 上传用户头像（包含删除旧头像、保存新头像、更新数据库）。
     *
     * @param userId 用户 ID
     * @param file   头像文件（webp 格式）
     * @return 头像相对路径（例如 /uploads/avatars/xxx.webp）
     * @throws IOException 文件操作异常
     */
    String uploadAvatar(Long userId, MultipartFile file) throws IOException;

    /**
     * 删除用户头像（删除文件并将数据库中的 avatar 字段设置为 null）。
     *
     * @param userId 用户 ID
     */
    void deleteAvatar(Long userId);

    /**
     * 获取当前用户的完整信息（包含角色列表）。
     *
     * @param userId 用户 ID
     * @return 当前用户信息 VO
     */
    CurrentUserVO getCurrentUserInfo(Long userId);

    /**
     * 获取当前用户的完整详情（包含 user 与 user_profile 扩展字段）。
     *
     * @param userId 用户 ID
     * @return 用户详情 VO
     */
    UserDetailVO getCurrentUserDetail(Long userId);

    /**
     * 更新用户个人信息（不允许修改 username、password、avatar）。
     *
     * @param userId 用户 ID
     * @param dto    更新信息 DTO
     */
    void updateProfile(Long userId, UserUpdateDTO dto);

    /**
     * 修改用户名。
     *
     * @param userId 用户 ID
     * @param dto    用户名更新 DTO
     */
    void updateUsername(Long userId, UsernameUpdateDTO dto);

    /**
     * 修改邮箱。
     *
     * @param userId 用户 ID
     * @param dto    邮箱更新 DTO
     */
    void updateEmail(Long userId, EmailUpdateDTO dto);

    /**
     * 修改手机号。
     *
     * @param userId 用户 ID
     * @param dto    手机号更新 DTO
     */
    void updatePhone(Long userId, PhoneUpdateDTO dto);

    /**
     * 修改密码。
     *
     * @param userId 用户 ID
     * @param dto    密码更新 DTO（包含旧密码和新密码）
     */
    void updatePassword(Long userId, PasswordUpdateDTO dto);

    /**
     * 注销账号（软删除）。
     *
     * @param userId 用户 ID
     */
    void deleteAccount(Long userId);
}

