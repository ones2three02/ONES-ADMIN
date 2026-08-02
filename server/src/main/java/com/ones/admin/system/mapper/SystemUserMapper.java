package com.ones.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ones.admin.system.entity.SystemUserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {

    @Update("""
            update sys_user
            set failed_login_count = failed_login_count + 1,
                locked_until = case
                    when failed_login_count + 1 >= #{maxFailedLoginCount} then #{lockedUntil}
                    else null
                end,
                updated_at = current_timestamp
            where id = #{userId}
            """)
    int incrementLoginFailure(
            @Param("userId") Long userId,
            @Param("maxFailedLoginCount") int maxFailedLoginCount,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );

    @Update("""
            update sys_user
            set failed_login_count = 0,
                locked_until = null,
                last_login_at = #{lastLoginAt},
                updated_at = #{lastLoginAt}
            where id = #{userId}
            """)
    int recordLoginSuccess(
            @Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt
    );
}
