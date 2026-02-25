package com.example.ojpt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ojpt.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

