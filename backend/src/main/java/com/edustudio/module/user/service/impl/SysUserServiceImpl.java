package com.edustudio.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edustudio.module.user.entity.SysUser;
import com.edustudio.module.user.mapper.SysUserMapper;
import com.edustudio.module.user.service.SysUserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Optional<SysUser> findByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0);
        return Optional.ofNullable(getOne(wrapper, false));
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0);
        return count(wrapper) > 0;
    }
}
