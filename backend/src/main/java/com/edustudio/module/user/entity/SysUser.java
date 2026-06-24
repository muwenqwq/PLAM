package com.edustudio.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.edustudio.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    private String email;

    private String phone;

    private String avatarUrl;

    private String userType;

    private String status;

    private LocalDateTime lastLoginAt;
}
