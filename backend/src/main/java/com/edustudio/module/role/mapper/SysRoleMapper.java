package com.edustudio.module.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edustudio.module.role.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.*
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND ur.deleted = 0
              AND ur.status = 'active'
              AND r.deleted = 0
              AND r.status = 'active'
            ORDER BY r.id ASC
            """)
    List<SysRole> selectActiveRolesByUserId(@Param("userId") Long userId);
}
