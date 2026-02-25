package com.example.ojpt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("class_user") // 班级-学员关系
public class ClassUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 主键
    private Long classId; // 班级ID
    private Long userId; // 学员ID
    private String joinType; // 加入方式：INVITE/ APPlY
    private String joinStatus; // 申请/邀请状态
    private LocalDateTime joinAt; // 加入时间
    private Long reviewerId; // 审核人/操作人
    private LocalDateTime reviewAt; // 审核时间
    private String reviewComment; // 审核备注
}

