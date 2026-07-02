package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemMenuEntity;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRoleMapper;
import com.ones.admin.system.mapper.SystemRolePermissionMapper;
import com.ones.admin.system.mapper.SystemUserMapper;
import com.ones.admin.system.mapper.SystemUserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class SystemDataInitializer implements ApplicationRunner {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final SystemUserMapper userMapper;
    private final SystemRoleMapper roleMapper;
    private final SystemPermissionMapper permissionMapper;
    private final SystemUserRoleMapper userRoleMapper;
    private final SystemRolePermissionMapper rolePermissionMapper;
    private final SystemDeptMapper deptMapper;
    private final SystemMenuMapper menuMapper;

    public SystemDataInitializer(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemPermissionMapper permissionMapper,
            SystemUserRoleMapper userRoleMapper,
            SystemRolePermissionMapper rolePermissionMapper,
            SystemDeptMapper deptMapper,
            SystemMenuMapper menuMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.deptMapper = deptMapper;
        this.menuMapper = menuMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        SystemRoleEntity superAdminRole = ensureRole("SUPER_ADMIN", "超级管理员", "拥有平台全部管理权限");
        SystemRoleEntity operatorRole = ensureRole("OPERATOR", "运营人员", "负责日常运营查看和基础维护");

        List<SystemPermissionEntity> permissions = List.of(
                ensurePermission("dashboard:view", "工作台查看"),
                ensurePermission("system:user:list", "用户查询"),
                ensurePermission("system:user:create", "用户新增"),
                ensurePermission("system:user:update", "用户编辑"),
                ensurePermission("system:user:delete", "用户删除"),
                ensurePermission("system:role:list", "角色查询"),
                ensurePermission("system:role:create", "角色新增"),
                ensurePermission("system:role:update", "角色编辑"),
                ensurePermission("system:role:delete", "角色删除"),
                ensurePermission("system:menu:list", "菜单查询"),
                ensurePermission("system:menu:create", "菜单新增"),
                ensurePermission("system:menu:update", "菜单编辑"),
                ensurePermission("system:menu:delete", "菜单删除"),
                ensurePermission("system:dept:list", "部门查询"),
                ensurePermission("system:dept:create", "部门新增"),
                ensurePermission("system:dept:update", "部门编辑"),
                ensurePermission("system:dept:delete", "部门删除"),
                ensurePermission("system:audit:login-log", "登录日志查询"),
                ensurePermission("system:audit:operation-log", "操作日志查询"),
                ensurePermission("system:audit:retention", "审计日志保留策略"),
                ensurePermission("system:api:list", "接口资源查询"),
                ensurePermission("system:api:publish", "接口资源发布"),
                ensurePermission("system:file:upload", "文件上传"),
                ensurePermission("hr:employee:list", "员工查询"),
                ensurePermission("hr:employee:detail", "员工详情"),
                ensurePermission("hr:employee:create", "员工新增"),
                ensurePermission("hr:position:list", "岗位查询"),
                ensurePermission("hr:position:create", "岗位新增"),
                ensurePermission("hr:position:update", "岗位编辑"),
                ensurePermission("hr:job-grade:list", "职级查询"),
                ensurePermission("hr:job-grade:create", "职级新增"),
                ensurePermission("hr:job-grade:update", "职级编辑")
        );

        permissions.forEach(permission -> ensureRolePermission(superAdminRole.getId(), permission.getId()));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "dashboard:view"));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "system:user:list"));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "system:dept:list"));

        SystemDeptEntity rootDept = ensureDept(null, "ONES 总部", "默认组织");
        ensureMenuTree();

        SystemUserEntity admin = ensureAdminUser();
        if (admin.getDeptId() == null) {
            admin.setDeptId(rootDept.getId());
            admin.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(admin);
        }
        ensureUserRole(admin.getId(), superAdminRole.getId());
    }

    private SystemRoleEntity ensureRole(String code, String name, String remark) {
        SystemRoleEntity existing = roleMapper.selectOne(new LambdaQueryWrapper<SystemRoleEntity>()
                .eq(SystemRoleEntity::getCode, code)
                .last("limit 1"));
        if (existing != null) {
            boolean changed = false;
            if (existing.getRemark() == null || existing.getRemark().isBlank()) {
                existing.setRemark(remark);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                roleMapper.updateById(existing);
            }
            return existing;
        }
        SystemRoleEntity role = new SystemRoleEntity();
        role.setCode(code);
        role.setName(name);
        role.setRemark(remark);
        role.setEnabled(true);
        roleMapper.insert(role);
        return role;
    }

    private SystemPermissionEntity ensurePermission(String code, String name) {
        SystemPermissionEntity existing = permissionMapper.selectOne(new LambdaQueryWrapper<SystemPermissionEntity>()
                .eq(SystemPermissionEntity::getCode, code)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemPermissionEntity permission = new SystemPermissionEntity();
        permission.setCode(code);
        permission.setName(name);
        permissionMapper.insert(permission);
        return permission;
    }

    private SystemUserEntity ensureAdminUser() {
        SystemUserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<SystemUserEntity>()
                .eq(SystemUserEntity::getUsername, "admin")
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemUserEntity user = new SystemUserEntity();
        user.setUsername("admin");
        user.setDisplayName("系统管理员");
        user.setRemark("系统内置管理员账号");
        user.setAvatar("https://api.dicebear.com/9.x/initials/svg?seed=ONES");
        user.setPasswordHash(PASSWORD_ENCODER.encode("admin123"));
        user.setEnabled(true);
        user.setFailedLoginCount(0);
        userMapper.insert(user);
        return user;
    }

    private void ensureUserRole(Long userId, Long roleId) {
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRoleEntity>()
                .eq(SystemUserRoleEntity::getUserId, userId)
                .eq(SystemUserRoleEntity::getRoleId, roleId));
        if (count > 0) {
            return;
        }
        SystemUserRoleEntity relation = new SystemUserRoleEntity();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        userRoleMapper.insert(relation);
    }

    private void ensureRolePermission(Long roleId, Long permissionId) {
        Long count = rolePermissionMapper.selectCount(new LambdaQueryWrapper<SystemRolePermissionEntity>()
                .eq(SystemRolePermissionEntity::getRoleId, roleId)
                .eq(SystemRolePermissionEntity::getPermissionId, permissionId));
        if (count > 0) {
            return;
        }
        SystemRolePermissionEntity relation = new SystemRolePermissionEntity();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        rolePermissionMapper.insert(relation);
    }

    private Long permissionIdByCode(List<SystemPermissionEntity> permissions, String code) {
        Map<String, Long> permissionIds = permissions.stream()
                .collect(java.util.stream.Collectors.toMap(SystemPermissionEntity::getCode, SystemPermissionEntity::getId));
        return permissionIds.get(code);
    }

    private SystemDeptEntity ensureDept(Long parentId, String name, String remark) {
        SystemDeptEntity existing = deptMapper.selectOne(new LambdaQueryWrapper<SystemDeptEntity>()
                .eq(SystemDeptEntity::getName, name)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        SystemDeptEntity dept = new SystemDeptEntity();
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setRemark(remark);
        dept.setEnabled(true);
        deptMapper.insert(dept);
        return dept;
    }

    private void ensureMenuTree() {
        SystemMenuEntity dashboard = ensureMenu(null, "Dashboard", "工作台", "/dashboard",
                "BasicLayout", "/dashboard/overview", null, "lucide:layout-dashboard", "catalog", -1);
        ensureMenu(dashboard.getId(), "SystemOverview", "系统概览", "/dashboard/overview",
                "/dashboard/overview/index", null, "dashboard:view", "lucide:gauge", "menu", 1);

        SystemMenuEntity system = ensureMenu(null, "System", "系统管理", "/system",
                "BasicLayout", null, null, "ion:settings-outline", "catalog", 100);
        SystemMenuEntity user = ensureMenu(system.getId(), "SystemUser", "用户管理", "/system/user",
                "/system/user/list", null, "system:user:list", "mdi:user", "menu", 10);
        ensureMenu(user.getId(), "SystemUserCreate", "新增用户", "/system/user#create",
                null, null, "system:user:create", "carbon:add", "button", 11);
        ensureMenu(user.getId(), "SystemUserUpdate", "编辑用户", "/system/user#update",
                null, null, "system:user:update", "carbon:edit", "button", 12);
        ensureMenu(user.getId(), "SystemUserDelete", "删除用户", "/system/user#delete",
                null, null, "system:user:delete", "carbon:trash-can", "button", 13);

        SystemMenuEntity role = ensureMenu(system.getId(), "SystemRole", "角色管理", "/system/role",
                "/system/role/list", null, "system:role:list", "mdi:account-group", "menu", 20);
        ensureMenu(role.getId(), "SystemRoleCreate", "新增角色", "/system/role#create",
                null, null, "system:role:create", "carbon:add", "button", 21);
        ensureMenu(role.getId(), "SystemRoleUpdate", "编辑角色", "/system/role#update",
                null, null, "system:role:update", "carbon:edit", "button", 22);
        ensureMenu(role.getId(), "SystemRoleDelete", "删除角色", "/system/role#delete",
                null, null, "system:role:delete", "carbon:trash-can", "button", 23);

        SystemMenuEntity menu = ensureMenu(system.getId(), "SystemMenu", "菜单管理", "/system/menu",
                "/system/menu/list", null, "system:menu:list", "mdi:menu", "menu", 30);
        ensureMenu(menu.getId(), "SystemMenuCreate", "新增菜单", "/system/menu#create",
                null, null, "system:menu:create", "carbon:add", "button", 31);
        ensureMenu(menu.getId(), "SystemMenuUpdate", "编辑菜单", "/system/menu#update",
                null, null, "system:menu:update", "carbon:edit", "button", 32);
        ensureMenu(menu.getId(), "SystemMenuDelete", "删除菜单", "/system/menu#delete",
                null, null, "system:menu:delete", "carbon:trash-can", "button", 33);

        SystemMenuEntity dept = ensureMenu(system.getId(), "SystemDept", "部门管理", "/system/dept",
                "/system/dept/list", null, "system:dept:list", "charm:organisation", "menu", 40);
        ensureMenu(dept.getId(), "SystemDeptCreate", "新增部门", "/system/dept#create",
                null, null, "system:dept:create", "carbon:add", "button", 41);
        ensureMenu(dept.getId(), "SystemDeptUpdate", "编辑部门", "/system/dept#update",
                null, null, "system:dept:update", "carbon:edit", "button", 42);
        ensureMenu(dept.getId(), "SystemDeptDelete", "删除部门", "/system/dept#delete",
                null, null, "system:dept:delete", "carbon:trash-can", "button", 43);

        SystemMenuEntity apiResources = ensureMenu(system.getId(), "SystemApiResources", "接口管理", "/system/api-resources",
                "/system/api-resource/list", null, "system:api:list", "lucide:network", "menu", 50);
        ensureMenu(apiResources.getId(), "SystemApiResourceList", "接口资源查询", "/system/api-resources#list",
                null, null, "system:api:list", "lucide:list-filter", "button", 51);
        ensureMenu(apiResources.getId(), "SystemApiResourcePublish", "接口资源发布", "/system/api-resources#publish",
                null, null, "system:api:publish", "lucide:badge-check", "button", 52);

        ensureMenu(system.getId(), "SystemAuditLoginLog", "登录日志查询", "/system#audit-login-log",
                null, null, "system:audit:login-log", "lucide:shield-check", "button", 60);
        ensureMenu(system.getId(), "SystemAuditOperationLog", "操作日志查询", "/system#audit-operation-log",
                null, null, "system:audit:operation-log", "lucide:scroll-text", "button", 61);
        ensureMenu(system.getId(), "SystemAuditRetention", "审计保留策略", "/system#audit-retention",
                null, null, "system:audit:retention", "lucide:archive-restore", "button", 62);
        ensureMenu(system.getId(), "SystemFileUpload", "文件上传", "/system#file-upload",
                null, null, "system:file:upload", "lucide:upload", "button", 63);

        ensureMenu(system.getId(), "HrEmployeeList", "员工查询", "/system#hr-employee-list",
                null, null, "hr:employee:list", "lucide:users", "button", 70);
        ensureMenu(system.getId(), "HrEmployeeDetail", "员工详情", "/system#hr-employee-detail",
                null, null, "hr:employee:detail", "lucide:user-search", "button", 71);
        ensureMenu(system.getId(), "HrEmployeeCreate", "员工新增", "/system#hr-employee-create",
                null, null, "hr:employee:create", "lucide:user-plus", "button", 72);
        ensureMenu(system.getId(), "HrPositionList", "岗位查询", "/system#hr-position-list",
                null, null, "hr:position:list", "lucide:briefcase-business", "button", 73);
        ensureMenu(system.getId(), "HrPositionCreate", "岗位新增", "/system#hr-position-create",
                null, null, "hr:position:create", "lucide:badge-plus", "button", 74);
        ensureMenu(system.getId(), "HrPositionUpdate", "岗位编辑", "/system#hr-position-update",
                null, null, "hr:position:update", "lucide:badge-pen", "button", 75);
        ensureMenu(system.getId(), "HrJobGradeList", "职级查询", "/system#hr-job-grade-list",
                null, null, "hr:job-grade:list", "lucide:layers-3", "button", 76);
        ensureMenu(system.getId(), "HrJobGradeCreate", "职级新增", "/system#hr-job-grade-create",
                null, null, "hr:job-grade:create", "lucide:layers-3", "button", 77);
        ensureMenu(system.getId(), "HrJobGradeUpdate", "职级编辑", "/system#hr-job-grade-update",
                null, null, "hr:job-grade:update", "lucide:layers-3", "button", 78);
    }

    private SystemMenuEntity ensureMenu(
            Long parentId,
            String name,
            String title,
            String path,
            String component,
            String redirect,
            String authCode,
            String icon,
            String type,
            int sortOrder
    ) {
        SystemMenuEntity existing = menuMapper.selectOne(new LambdaQueryWrapper<SystemMenuEntity>()
                .eq(SystemMenuEntity::getName, name)
                .last("limit 1"));
        if (existing != null) {
            existing.setParentId(parentId);
            existing.setTitle(title);
            existing.setPath(path);
            existing.setComponent(component);
            existing.setRedirect(redirect);
            existing.setAuthCode(authCode);
            existing.setIcon(icon);
            existing.setType(type);
            existing.setSortOrder(sortOrder);
            existing.setEnabled(true);
            existing.setUpdatedAt(LocalDateTime.now());
            menuMapper.updateById(existing);
            return existing;
        }
        SystemMenuEntity menu = new SystemMenuEntity();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setTitle(title);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setRedirect(redirect);
        menu.setAuthCode(authCode);
        menu.setIcon(icon);
        menu.setType(type);
        menu.setSortOrder(sortOrder);
        menu.setEnabled(true);
        menuMapper.insert(menu);
        return menu;
    }
}
