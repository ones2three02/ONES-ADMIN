package com.ones.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ones.admin.system.entity.SystemRoleEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SystemRoleMapper extends BaseMapper<SystemRoleEntity> {

    @Select("""
            select r.code
            from sys_role r
            inner join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
              and r.enabled = true
            order by r.id asc
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    @Select("""
            select r.data_scope
            from sys_role r
            inner join sys_user_role ur on ur.role_id = r.id
            where ur.user_id = #{userId}
              and r.enabled = true
            order by r.id asc
            """)
    List<String> selectDataScopesByUserId(Long userId);
}
