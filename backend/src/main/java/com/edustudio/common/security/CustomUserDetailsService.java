package com.edustudio.common.security;

import com.edustudio.module.role.entity.SysRole;
import com.edustudio.module.role.mapper.SysRoleMapper;
import com.edustudio.module.user.entity.SysUser;
import com.edustudio.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        List<String> roles = sysRoleMapper.selectActiveRolesByUserId(user.getId())
                .stream()
                .map(SysRole::getRoleCode)
                .toList();
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getStatus(),
                roles
        );
    }
}
