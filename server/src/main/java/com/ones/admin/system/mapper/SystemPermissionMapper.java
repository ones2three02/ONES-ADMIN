package com.ones.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ones.admin.system.entity.SystemPermissionEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SystemPermissionMapper extends BaseMapper<SystemPermissionEntity> {

    @Select("""
            select p.code
            from sys_permission p
            order by p.code asc
            """)
    List<String> selectAllCodes();

    @Select("""
            select distinct p.code
            from sys_permission p
            inner join sys_role_permission rp on rp.permission_id = p.id
            inner join sys_user_role ur on ur.role_id = rp.role_id
            inner join sys_role r on r.id = ur.role_id
            where ur.user_id = #{userId}
              and r.enabled = true
            order by p.code asc
            """)
    List<String> selectPermissionCodesByUserId(Long userId);
}
