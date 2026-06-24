package com.edustudio.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.edustudio.module.user.entity.SysUser;

import java.util.Optional;

public interface SysUserService extends IService<SysUser> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
