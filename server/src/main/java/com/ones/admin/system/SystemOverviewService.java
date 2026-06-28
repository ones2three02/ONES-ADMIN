package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.dto.SystemOverviewResponse;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemMenuEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SystemOverviewService {

    private final SystemUserMapper userMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemMenuMapper menuMapper;
    private final SystemDeptMapper deptMapper;

    public SystemOverviewService(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemMenuMapper menuMapper,
            SystemDeptMapper deptMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.deptMapper = deptMapper;
    }

    public SystemOverviewResponse getOverview() {
        return new SystemOverviewResponse(
                userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()),
                roleMapper.selectCount(new LambdaQueryWrapper<SystemRoleEntity>()),
                menuMapper.selectCount(new LambdaQueryWrapper<SystemMenuEntity>()),
                deptMapper.selectCount(new LambdaQueryWrapper<SystemDeptEntity>()),
                userMapper.selectCount(new LambdaQueryWrapper<SystemUserEntity>()
                        .eq(SystemUserEntity::getEnabled, true)),
                LocalDateTime.now()
        );
    }
}
