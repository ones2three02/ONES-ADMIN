package com.ones.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ones.admin.system.entity.SystemMenuEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SystemMenuMapper extends BaseMapper<SystemMenuEntity> {

    @Select("""
            select distinct m.auth_code
            from sys_menu m
            where m.auth_code is not null
              and m.auth_code <> ''
            order by m.auth_code asc
            """)
    List<String> selectAssignablePermissionCodes();

    @Select("""
            select m.id
            from sys_menu m
            inner join sys_permission p on p.code = m.auth_code
            inner join sys_role_permission rp on rp.permission_id = p.id
            where rp.role_id = #{roleId}
              and m.auth_code is not null
            order by m.sort_order asc, m.id asc
            """)
    List<Long> selectMenuIdsByRoleId(Long roleId);
}
