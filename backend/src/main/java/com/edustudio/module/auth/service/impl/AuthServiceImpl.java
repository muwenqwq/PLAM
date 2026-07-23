package com.edustudio.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edustudio.common.api.ResultCode;
import com.edustudio.common.exception.BusinessException;
import com.edustudio.common.security.JwtTokenUtil;
import com.edustudio.common.security.LoginUserHolder;
import com.edustudio.common.security.TokenBlacklistService;
import com.edustudio.common.security.UserPrincipal;
import com.edustudio.module.auth.dto.LoginRequest;
import com.edustudio.module.auth.dto.RegisterRequest;
import com.edustudio.module.auth.service.AuthService;
import com.edustudio.module.auth.vo.CurrentUserVO;
import com.edustudio.module.auth.vo.LoginResponse;
import com.edustudio.module.role.entity.SysRole;
import com.edustudio.module.role.entity.SysUserRole;
import com.edustudio.module.role.mapper.SysRoleMapper;
import com.edustudio.module.role.mapper.SysUserRoleMapper;
import com.edustudio.module.user.entity.SysUser;
import com.edustudio.module.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ACTIVE_STATUS = "active";
    private static final String DEFAULT_USER_TYPE = "student";
    private static final String DEFAULT_ROLE_CODE = "STUDENT";

    private final SysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CurrentUserVO register(RegisterRequest request) {
        if (sysUserService.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setUserType(DEFAULT_USER_TYPE);
        user.setStatus(ACTIVE_STATUS);
        sysUserService.save(user);

        assignDefaultRole(user.getId());
        return toCurrentUserVO(user, activeRoleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserService.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名或密码错误");
        }

        List<String> roles = activeRoleCodes(user.getId());
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getNickname(),
                user.getStatus(),
                roles
        );
        String token = jwtTokenUtil.generateToken(principal);
        user.setLastLoginAt(LocalDateTime.now());
        sysUserService.updateById(user);

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(token)
                .expiresAt(jwtTokenUtil.getExpiresAt(token))
                .user(toCurrentUserVO(user, roles))
                .build();
    }

    @Override
    public CurrentUserVO currentUser() {
        UserPrincipal principal = LoginUserHolder.requireCurrentUser();
        SysUser user = sysUserService.getById(principal.getUserId());
        if (user == null || !ACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户不存在或已禁用");
        }
        return toCurrentUserVO(user, activeRoleCodes(user.getId()));
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token) && jwtTokenUtil.validateToken(token)) {
            tokenBlacklistService.blacklist(token, jwtTokenUtil.getExpiresAt(token));
        }
        LoginUserHolder.clear();
    }

    private void assignDefaultRole(Long userId) {
        SysRole role = selectRoleByCode(DEFAULT_ROLE_CODE);
        if (role == null) {
            return;
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setStatus(ACTIVE_STATUS);
        sysUserRoleMapper.insert(userRole);
    }

    private SysRole selectRoleByCode(String roleCode) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getStatus, ACTIVE_STATUS)
                .eq(SysRole::getDeleted, 0);
        List<SysRole> roles = sysRoleMapper.selectList(wrapper);
        return roles.isEmpty() ? null : roles.getFirst();
    }

    private List<String> activeRoleCodes(Long userId) {
        return sysRoleMapper.selectActiveRolesByUserId(userId)
                .stream()
                .map(SysRole::getRoleCode)
                .toList();
    }

    private CurrentUserVO toCurrentUserVO(SysUser user, List<String> roles) {
        return CurrentUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .userType(user.getUserType())
                .status(user.getStatus())
                .roles(roles)
                .build();
    }
}
