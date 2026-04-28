package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.ojpt.dto.EmailUpdateDTO;
import com.example.ojpt.dto.PasswordUpdateDTO;
import com.example.ojpt.dto.PhoneUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.dto.UsernameUpdateDTO;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserProfile;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.config.JwtProperties;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserProfileMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.security.SystemRoleScope;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.CurrentUserVO;
import com.example.ojpt.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Value("${ojpt.upload.base-path}")
    private String avatarBasePath;

    @Value("${ojpt.upload.avatar-path}")
    private String avatarSubDir;

    @Value("${ojpt.upload.avatar-url-prefix}")
    private String avatarUrlPrefix;

    @Override
    public User findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1"));
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1"));
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        // 注意：MyBatis-Plus 默认的 updateStrategy 为 NOT_NULL，直接使用 updateById(user)
        // 会忽略 avatarUrl 为 null 的情况，导致无法将 avatar 字段更新为 null。
        // 这里使用 UpdateWrapper 显式设置 avatar 字段，即使为 null 也会更新。
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", userId).set("avatar", avatarUrl);
        userMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        // 查询用户当前头像，如果存在则删除旧头像文件
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 删除旧头像文件
        deleteAvatarFile(userId, currentUser.getAvatar());

        // 构造保存路径与文件名：{userId}_{timestamp}.webp
        Path avatarDir = Paths.get(avatarBasePath, avatarSubDir);
        Files.createDirectories(avatarDir);

        String filename = userId + "_" + System.currentTimeMillis() + ".webp";
        Path target = avatarDir.resolve(filename).normalize();

        // 保存新头像文件
        file.transferTo(target.toFile());

        // 数据库存储相对 URL，供前端直接通过 nginx 访问
        String avatarUrl = avatarUrlPrefix + "/" + filename;
        updateAvatar(userId, avatarUrl);

        return avatarUrl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAvatar(Long userId) {
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 删除头像文件
        deleteAvatarFile(userId, currentUser.getAvatar());

        // 将数据库中的 avatar 字段设置为 null
        updateAvatar(userId, null);
    }

    /**
     * 删除头像文件的辅助方法。
     *
     * @param userId    用户 ID
     * @param avatarUrl 头像 URL（可能为 null）
     */
    private void deleteAvatarFile(Long userId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        try {
            // 从 avatarUrl 中提取文件名（例如：/uploads/avatars/xxx.webp -> xxx.webp）
            String oldFilename = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
            Path oldAvatarPath = Paths.get(avatarBasePath, avatarSubDir, oldFilename).normalize();

            // 检查文件是否存在且属于当前用户（防止删除其他用户的文件）
            if (Files.exists(oldAvatarPath) && oldFilename.startsWith(userId + "_")) {
                Files.delete(oldAvatarPath);
            }
        } catch (IOException e) {
            // 删除旧头像失败不影响操作，记录日志即可
            // 可以在这里添加日志记录：logger.warn("删除旧头像失败: {}", e.getMessage());
        }
    }

    @Override
    public CurrentUserVO getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 查询用户的所有角色
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));
        
        Set<String> roleCodes = userRoles.stream()
                .map(UserRole::getRoleId)
                .map(roleMapper::selectById)
                .filter(role -> role != null)
                .map(Role::getCode)
                .collect(Collectors.toSet());
        
        // 如果用户有 roleType，也加入角色列表
        if (user.getRoleType() != null) {
            roleCodes.add(user.getRoleType());
        }
        
        List<String> roles = SystemRoleScope.normalizeRoleCodes(roleCodes);

        return new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                SystemRoleScope.normalizeRoleType(user.getRoleType()),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Override
    public UserDetailVO getCurrentUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 查询用户的所有角色
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));

        Set<String> roleCodes = userRoles.stream()
                .map(UserRole::getRoleId)
                .map(roleMapper::selectById)
                .filter(role -> role != null)
                .map(Role::getCode)
                .collect(Collectors.toSet());

        // 如果用户有 roleType，也加入角色列表
        if (user.getRoleType() != null) {
            roleCodes.add(user.getRoleType());
        }

        List<String> roles = SystemRoleScope.normalizeRoleCodes(roleCodes);

        // 查询用户扩展信息（可能不存在）
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));

        UserDetailVO vo = new UserDetailVO();
        // 基础信息
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoleType(SystemRoleScope.normalizeRoleType(user.getRoleType()));
        vo.setStatus(user.getStatus());
        vo.setRoles(roles);
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());

        // 扩展信息（可能为 null）
        if (profile != null) {
            vo.setGender(profile.getGender());
            vo.setBirthday(profile.getBirthday());
            vo.setAddress(profile.getAddress());
            vo.setWebsite(profile.getWebsite());
            vo.setGithub(profile.getGithub());
            vo.setCompany(profile.getCompany());
            vo.setPosition(profile.getPosition());
            vo.setSkills(profile.getSkills());
            vo.setStudentNo(profile.getStudentNo());
            vo.setSchoolId(profile.getSchoolId());
            vo.setBio(profile.getBio());
            vo.setTags(profile.getTags());
            vo.setIdentityStatus(profile.getIdentityStatus());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 校验邮箱唯一性（如果提供了新邮箱且与当前邮箱不同）
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!dto.getEmail().equals(user.getEmail())) {
                User existingUser = findByEmail(dto.getEmail());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被使用");
                }
            }
        }

        // 校验手机号唯一性（如果提供了新手机号且与当前手机号不同）
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            if (!dto.getPhone().equals(user.getPhone())) {
                User existingUser = findByPhone(dto.getPhone());
                if (existingUser != null && !existingUser.getId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已被使用");
                }
            }
        }

        // 更新用户基础信息
        User updateUser = new User();
        updateUser.setId(userId);
        if (dto.getEmail() != null) {
            updateUser.setEmail(dto.getEmail().isBlank() ? null : dto.getEmail());
        }
        if (dto.getPhone() != null) {
            updateUser.setPhone(dto.getPhone().isBlank() ? null : dto.getPhone());
        }
        userMapper.updateById(updateUser);

        // 更新或创建用户扩展信息（UserProfile）
        UserProfile existingProfile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));

        if (existingProfile != null) {
            // 更新现有 profile
            UserProfile updateProfile = new UserProfile();
            updateProfile.setId(existingProfile.getId());
            
            if (dto.getGender() != null) {
                updateProfile.setGender(dto.getGender());
            }
            if (dto.getBirthday() != null) {
                updateProfile.setBirthday(dto.getBirthday());
            }
            if (dto.getAddress() != null) {
                updateProfile.setAddress(dto.getAddress().isBlank() ? null : dto.getAddress());
            }
            if (dto.getWebsite() != null) {
                updateProfile.setWebsite(dto.getWebsite().isBlank() ? null : dto.getWebsite());
            }
            if (dto.getGithub() != null) {
                updateProfile.setGithub(dto.getGithub().isBlank() ? null : dto.getGithub());
            }
            if (dto.getCompany() != null) {
                updateProfile.setCompany(dto.getCompany().isBlank() ? null : dto.getCompany());
            }
            if (dto.getPosition() != null) {
                updateProfile.setPosition(dto.getPosition().isBlank() ? null : dto.getPosition());
            }
            if (dto.getSkills() != null) {
                updateProfile.setSkills(dto.getSkills().isBlank() ? null : dto.getSkills());
            }
            if (dto.getStudentNo() != null) {
                updateProfile.setStudentNo(dto.getStudentNo().isBlank() ? null : dto.getStudentNo());
            }
            if (dto.getSchoolId() != null) {
                updateProfile.setSchoolId(dto.getSchoolId());
            }
            if (dto.getBio() != null) {
                updateProfile.setBio(dto.getBio().isBlank() ? null : dto.getBio());
            }
            if (dto.getTags() != null) {
                updateProfile.setTags(dto.getTags().isBlank() ? null : dto.getTags());
            }
            
            userProfileMapper.updateById(updateProfile);
        } else {
            // 创建新的 profile（仅当至少有一个 profile 字段被提供时）
            boolean hasProfileData = dto.getGender() != null
                    || dto.getBirthday() != null
                    || (dto.getAddress() != null && !dto.getAddress().isBlank())
                    || (dto.getWebsite() != null && !dto.getWebsite().isBlank())
                    || (dto.getGithub() != null && !dto.getGithub().isBlank())
                    || (dto.getCompany() != null && !dto.getCompany().isBlank())
                    || (dto.getPosition() != null && !dto.getPosition().isBlank())
                    || (dto.getSkills() != null && !dto.getSkills().isBlank())
                    || (dto.getStudentNo() != null && !dto.getStudentNo().isBlank())
                    || dto.getSchoolId() != null
                    || (dto.getBio() != null && !dto.getBio().isBlank())
                    || (dto.getTags() != null && !dto.getTags().isBlank());

            if (hasProfileData) {
                UserProfile newProfile = new UserProfile()
                        .setUserId(userId)
                        .setGender(dto.getGender())
                        .setBirthday(dto.getBirthday())
                        .setAddress(dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress() : null)
                        .setWebsite(dto.getWebsite() != null && !dto.getWebsite().isBlank() ? dto.getWebsite() : null)
                        .setGithub(dto.getGithub() != null && !dto.getGithub().isBlank() ? dto.getGithub() : null)
                        .setCompany(dto.getCompany() != null && !dto.getCompany().isBlank() ? dto.getCompany() : null)
                        .setPosition(dto.getPosition() != null && !dto.getPosition().isBlank() ? dto.getPosition() : null)
                        .setSkills(dto.getSkills() != null && !dto.getSkills().isBlank() ? dto.getSkills() : null)
                        .setStudentNo(dto.getStudentNo() != null && !dto.getStudentNo().isBlank() ? dto.getStudentNo() : null)
                        .setSchoolId(dto.getSchoolId())
                        .setBio(dto.getBio() != null && !dto.getBio().isBlank() ? dto.getBio() : null)
                        .setTags(dto.getTags() != null && !dto.getTags().isBlank() ? dto.getTags() : null);
                userProfileMapper.insert(newProfile);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUsername(Long userId, UsernameUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        String newUsername = dto.getUsername().trim();
        
        // 如果新用户名与当前用户名相同，直接返回
        if (newUsername.equals(user.getUsername())) {
            return;
        }

        // 校验用户名唯一性
        User existingUser = findByUsername(newUsername);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被使用");
        }

        // 更新用户名
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUsername(newUsername);
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(Long userId, EmailUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        String newEmail = dto.getEmail().trim().toLowerCase();
        
        // 如果新邮箱与当前邮箱相同，直接返回
        if (newEmail.equals(user.getEmail())) {
            return;
        }

        // 校验邮箱唯一性
        User existingUser = findByEmail(newEmail);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被使用");
        }

        // 更新邮箱
        // TODO: 未来可添加邮箱验证码验证逻辑
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail(newEmail);
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(Long userId, PhoneUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        String newPhone = dto.getPhone().trim();
        
        // 如果新手机号与当前手机号相同，直接返回
        if (newPhone.equals(user.getPhone())) {
            return;
        }

        // 校验手机号唯一性
        User existingUser = findByPhone(newPhone);
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已被使用");
        }

        // 更新手机号
        // TODO: 未来可添加手机号验证码验证逻辑
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPhone(newPhone);
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        }

        // 校验新密码不能与旧密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }

        // 加密新密码并更新
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(encodedPassword);
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 软删除：将用户状态标记为已注销（status = 0）
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setStatus(0);
        userMapper.updateById(updateUser);

        // 清理该用户的所有 refresh token
        // 由于 RefreshTokenStore 的 key 格式是 prefix + userId + ":" + jti
        // 我们需要删除所有以 prefix + userId + ":" 开头的 refresh token
        String pattern = jwtProperties.getRedisPrefix() + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // TODO: 可选：保留数据一段时间（如 30 天）后再真正删除
        // 可以通过定时任务实现，定期清理已注销超过 30 天的用户数据
    }
}
