package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemDictItemEntity;
import com.ones.admin.system.entity.SystemDictTypeEntity;
import com.ones.admin.system.entity.SystemMenuEntity;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRoleEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.entity.SystemUserEntity;
import com.ones.admin.system.entity.SystemUserRoleEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemDictItemMapper;
import com.ones.admin.system.mapper.SystemDictTypeMapper;
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
    private final SystemDictTypeMapper dictTypeMapper;
    private final SystemDictItemMapper dictItemMapper;
    private final SystemMenuMapper menuMapper;

    public SystemDataInitializer(
            SystemUserMapper userMapper,
            SystemRoleMapper roleMapper,
            SystemPermissionMapper permissionMapper,
            SystemUserRoleMapper userRoleMapper,
            SystemRolePermissionMapper rolePermissionMapper,
            SystemDeptMapper deptMapper,
            SystemDictTypeMapper dictTypeMapper,
            SystemDictItemMapper dictItemMapper,
            SystemMenuMapper menuMapper
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.deptMapper = deptMapper;
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
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
                ensurePermission("system:dict:type:list", "字典类型查询"),
                ensurePermission("system:dict:type:create", "字典类型新增"),
                ensurePermission("system:dict:type:update", "字典类型编辑"),
                ensurePermission("system:dict:type:delete", "字典类型删除"),
                ensurePermission("system:dict:item:list", "字典项查询"),
                ensurePermission("system:dict:item:create", "字典项新增"),
                ensurePermission("system:dict:item:update", "字典项编辑"),
                ensurePermission("system:dict:item:delete", "字典项删除"),
                ensurePermission("system:file:upload", "文件上传"),
                ensurePermission("hr:overview:view", "人力概览查看"),
                ensurePermission("hr:employee:list", "员工查询"),
                ensurePermission("hr:employee:detail", "员工详情"),
                ensurePermission("hr:employee:sensitive:view", "员工敏感信息查看"),
                ensurePermission("hr:employee:lifecycle", "员工生命周期查询"),
                ensurePermission("hr:employee:create", "员工新增"),
                ensurePermission("hr:employee:update", "员工编辑"),
                ensurePermission("hr:employee:transfer", "员工调岗"),
                ensurePermission("hr:employee:regularize", "员工转正"),
                ensurePermission("hr:employee:resign", "员工离职"),
                ensurePermission("hr:employee:export", "员工花名册导出"),
                ensurePermission("hr:position:list", "岗位查询"),
                ensurePermission("hr:position:create", "岗位新增"),
                ensurePermission("hr:position:update", "岗位编辑"),
                ensurePermission("hr:job-grade:list", "职级查询"),
                ensurePermission("hr:job-grade:create", "职级新增"),
                ensurePermission("hr:job-grade:update", "职级编辑"),
                ensurePermission("hr:contract:list", "员工合同查询"),
                ensurePermission("hr:contract:create", "员工合同新增"),
                ensurePermission("hr:contract:update", "员工合同编辑"),
                ensurePermission("hr:contract:terminate", "员工合同终止"),
                ensurePermission("hr:roster:list", "花名册导入批次查询"),
                ensurePermission("hr:roster:import", "花名册导入")
        );

        permissions.forEach(permission -> ensureRolePermission(superAdminRole.getId(), permission.getId()));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "dashboard:view"));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "system:user:list"));
        ensureRolePermission(operatorRole.getId(), permissionIdByCode(permissions, "system:dept:list"));

        SystemDeptEntity rootDept = ensureDept(null, "ONES 总部", "默认组织");
        ensureDefaultDictionaries();
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
            String dataScope = "SUPER_ADMIN".equals(code) ? DataScope.ALL.name() : DataScope.DEPT_AND_CHILD.name();
            if (existing.getDataScope() == null || existing.getDataScope().isBlank()) {
                existing.setDataScope(dataScope);
                changed = true;
            }
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
        role.setDataScope("SUPER_ADMIN".equals(code) ? DataScope.ALL.name() : DataScope.DEPT_AND_CHILD.name());
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

        SystemMenuEntity dict = ensureMenu(system.getId(), "SystemDict", "数据字典", "/system/dict",
                "/system/dict/list", null, "system:dict:type:list", "lucide:book-open-text", "menu", 55);
        ensureMenu(dict.getId(), "SystemDictTypeList", "字典类型查询", "/system/dict#type-list",
                null, null, "system:dict:type:list", "lucide:list-filter", "button", 56);
        ensureMenu(dict.getId(), "SystemDictTypeCreate", "字典类型新增", "/system/dict#type-create",
                null, null, "system:dict:type:create", "carbon:add", "button", 57);
        ensureMenu(dict.getId(), "SystemDictTypeUpdate", "字典类型编辑", "/system/dict#type-update",
                null, null, "system:dict:type:update", "carbon:edit", "button", 58);
        ensureMenu(dict.getId(), "SystemDictTypeDelete", "字典类型删除", "/system/dict#type-delete",
                null, null, "system:dict:type:delete", "carbon:trash-can", "button", 59);
        ensureMenu(dict.getId(), "SystemDictItemList", "字典项查询", "/system/dict#item-list",
                null, null, "system:dict:item:list", "lucide:list-tree", "button", 60);
        ensureMenu(dict.getId(), "SystemDictItemCreate", "字典项新增", "/system/dict#item-create",
                null, null, "system:dict:item:create", "carbon:add", "button", 61);
        ensureMenu(dict.getId(), "SystemDictItemUpdate", "字典项编辑", "/system/dict#item-update",
                null, null, "system:dict:item:update", "carbon:edit", "button", 62);
        ensureMenu(dict.getId(), "SystemDictItemDelete", "字典项删除", "/system/dict#item-delete",
                null, null, "system:dict:item:delete", "carbon:trash-can", "button", 63);

        SystemMenuEntity audit = ensureMenu(system.getId(), "SystemAudit", "审计日志", "/system/audit",
                "/system/audit/list", null, "system:audit:login-log", "lucide:shield-check", "menu", 70);
        ensureMenu(audit.getId(), "SystemAuditLoginLog", "登录日志查询", "/system/audit#login-log",
                null, null, "system:audit:login-log", "lucide:log-in", "button", 71);
        ensureMenu(audit.getId(), "SystemAuditOperationLog", "操作日志查询", "/system/audit#operation-log",
                null, null, "system:audit:operation-log", "lucide:scroll-text", "button", 72);
        ensureMenu(audit.getId(), "SystemAuditRetention", "审计保留策略", "/system/audit#retention",
                null, null, "system:audit:retention", "lucide:archive-restore", "button", 73);
        ensureMenu(system.getId(), "SystemFileUpload", "文件上传", "/system#file-upload",
                null, null, "system:file:upload", "lucide:upload", "button", 74);

        SystemMenuEntity hrms = ensureMenu(null, "HRMS", "人力资源管理", "/hr",
                "BasicLayout", null, null, "lucide:users", "catalog", 200);

        ensureMenu(hrms.getId(), "HrOverview", "人力概览", "/hr/overview",
                "/hr/overview/index", null, "hr:overview:view", "lucide:chart-no-axes-combined", "menu", 5);

        SystemMenuEntity hrEmployee = ensureMenu(hrms.getId(), "HrEmployee", "员工管理", "/hr/employee",
                "/hr/employee/list", null, "hr:employee:list", "lucide:users", "menu", 10);
        ensureMenu(hrEmployee.getId(), "HrEmployeeDetail", "员工详情", "/hr/employee#detail",
                null, null, "hr:employee:detail", "lucide:user-search", "button", 11);
        ensureMenu(hrEmployee.getId(), "HrEmployeeSensitiveView", "员工敏感信息查看", "/hr/employee#sensitive-view",
                null, null, "hr:employee:sensitive:view", "lucide:eye", "button", 12);
        ensureMenu(hrEmployee.getId(), "HrEmployeeLifecycle", "员工生命周期", "/hr/employee#lifecycle",
                null, null, "hr:employee:lifecycle", "lucide:history", "button", 13);
        ensureMenu(hrEmployee.getId(), "HrEmployeeCreate", "员工新增", "/hr/employee#create",
                null, null, "hr:employee:create", "lucide:user-plus", "button", 14);
        ensureMenu(hrEmployee.getId(), "HrEmployeeUpdate", "员工编辑", "/hr/employee#update",
                null, null, "hr:employee:update", "lucide:user-pen", "button", 15);
        ensureMenu(hrEmployee.getId(), "HrEmployeeTransfer", "员工调岗", "/hr/employee#transfer",
                null, null, "hr:employee:transfer", "lucide:shuffle", "button", 16);
        ensureMenu(hrEmployee.getId(), "HrEmployeeRegularize", "员工转正", "/hr/employee#regularize",
                null, null, "hr:employee:regularize", "lucide:badge-check", "button", 17);
        ensureMenu(hrEmployee.getId(), "HrEmployeeResign", "员工离职", "/hr/employee#resign",
                null, null, "hr:employee:resign", "lucide:user-minus", "button", 18);
        ensureMenu(hrEmployee.getId(), "HrEmployeeExport", "员工花名册导出", "/hr/employee#export",
                null, null, "hr:employee:export", "lucide:download", "button", 19);

        SystemMenuEntity hrPosition = ensureMenu(hrms.getId(), "HrPosition", "岗位管理", "/hr/position",
                "/hr/position/list", null, "hr:position:list", "lucide:briefcase-business", "menu", 20);
        ensureMenu(hrPosition.getId(), "HrPositionCreate", "岗位新增", "/hr/position#create",
                null, null, "hr:position:create", "lucide:badge-plus", "button", 21);
        ensureMenu(hrPosition.getId(), "HrPositionUpdate", "岗位编辑", "/hr/position#update",
                null, null, "hr:position:update", "lucide:badge-pen", "button", 22);

        SystemMenuEntity hrJobGrade = ensureMenu(hrms.getId(), "HrJobGrade", "职级管理", "/hr/job-grade",
                "/hr/job-grade/list", null, "hr:job-grade:list", "lucide:layers-3", "menu", 30);
        ensureMenu(hrJobGrade.getId(), "HrJobGradeCreate", "职级新增", "/hr/job-grade#create",
                null, null, "hr:job-grade:create", "lucide:layers-3", "button", 31);
        ensureMenu(hrJobGrade.getId(), "HrJobGradeUpdate", "职级编辑", "/hr/job-grade#update",
                null, null, "hr:job-grade:update", "lucide:layers-3", "button", 32);

        SystemMenuEntity hrContract = ensureMenu(hrms.getId(), "HrContract", "合同管理", "/hr/contract",
                "/hr/contract/list", null, "hr:contract:list", "lucide:file-text", "menu", 40);
        ensureMenu(hrContract.getId(), "HrContractCreate", "合同新增", "/hr/contract#create",
                null, null, "hr:contract:create", "lucide:file-plus-2", "button", 41);
        ensureMenu(hrContract.getId(), "HrContractUpdate", "合同编辑", "/hr/contract#update",
                null, null, "hr:contract:update", "lucide:file-pen-line", "button", 42);
        ensureMenu(hrContract.getId(), "HrContractTerminate", "合同终止", "/hr/contract#terminate",
                null, null, "hr:contract:terminate", "lucide:file-x-2", "button", 43);

        SystemMenuEntity hrRosterImport = ensureMenu(hrms.getId(), "HrRosterImport", "花名册导入", "/hr/roster-import",
                "/hr/roster-import/list", null, "hr:roster:list", "lucide:file-up", "menu", 50);
        ensureMenu(hrRosterImport.getId(), "HrRosterImportList", "导入批次查询", "/hr/roster-import#list",
                null, null, "hr:roster:list", "lucide:file-search", "button", 51);
        ensureMenu(hrRosterImport.getId(), "HrRosterImportExecute", "执行导入", "/hr/roster-import#import",
                null, null, "hr:roster:import", "lucide:file-up", "button", 52);
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

    private void ensureDefaultDictionaries() {
        SystemDictTypeEntity employmentType = ensureDictType("hr_employment_type", "用工类型", "HRMS 员工用工类型", 10);
        ensureDictItem(employmentType, "全职(正式)", "FULL_TIME", "success", 10);
        ensureDictItem(employmentType, "兼职", "PART_TIME", "blue", 20);
        ensureDictItem(employmentType, "实习生", "INTERN", "processing", 30);
        ensureDictItem(employmentType, "劳务外包", "OUTSOURCED", "warning", 40);

        SystemDictTypeEntity employmentStatus = ensureDictType("hr_employment_status", "员工状态", "HRMS 员工任职状态", 20);
        ensureDictItem(employmentStatus, "在职(正式)", "ACTIVE", "success", 10);
        ensureDictItem(employmentStatus, "试用期", "PROBATION", "processing", 20);
        ensureDictItem(employmentStatus, "停职", "SUSPENDED", "warning", 30);
        ensureDictItem(employmentStatus, "已离职", "RESIGNED", "error", 40);

        SystemDictTypeEntity gender = ensureDictType("hr_gender", "性别", "HRMS 员工性别", 30);
        ensureDictItem(gender, "男", "MALE", "blue", 10);
        ensureDictItem(gender, "女", "FEMALE", "magenta", 20);

        SystemDictTypeEntity contractType = ensureDictType("hr_contract_type", "合同类型", "HRMS 员工合同类型", 40);
        ensureDictItem(contractType, "固定期限劳动合同", "FIXED_TERM", "processing", 10);
        ensureDictItem(contractType, "无固定期限劳动合同", "OPEN_ENDED", "success", 20);
        ensureDictItem(contractType, "实习协议", "INTERNSHIP", "blue", 30);
        ensureDictItem(contractType, "劳务合同", "SERVICE", "warning", 40);
        disableDictItem(contractType.getDictCode(), "FIXED");
        disableDictItem(contractType.getDictCode(), "UNFIXED");
        disableDictItem(contractType.getDictCode(), "DISPATCH");
        disableDictItem(contractType.getDictCode(), "INTERN");

        SystemDictTypeEntity contractStatus = ensureDictType("hr_contract_status", "合同状态", "HRMS 员工合同状态", 50);
        ensureDictItem(contractStatus, "草稿", "DRAFT", "default", 10);
        ensureDictItem(contractStatus, "履约中", "ACTIVE", "success", 20);
        ensureDictItem(contractStatus, "即将到期", "EXPIRING", "warning", 30);
        ensureDictItem(contractStatus, "已终止", "TERMINATED", "error", 40);
        disableDictItem(contractStatus.getDictCode(), "EXPIRED");

        SystemDictTypeEntity rosterImportStatus = ensureDictType("hr_roster_import_status", "花名册导入状态", "HRMS 花名册导入批次状态", 60);
        ensureDictItem(rosterImportStatus, "解析中", "PARSING", "processing", 10);
        ensureDictItem(rosterImportStatus, "校验失败", "VALIDATION_FAILED", "error", 20);
        ensureDictItem(rosterImportStatus, "部分成功", "PARTIAL_SUCCESS", "warning", 30);
        ensureDictItem(rosterImportStatus, "导入成功", "SUCCESS", "success", 40);
        ensureDictItem(rosterImportStatus, "导入失败", "FAILED", "error", 50);
        disableDictItem(rosterImportStatus.getDictCode(), "PENDING");
    }

    private SystemDictTypeEntity ensureDictType(String dictCode, String dictName, String remark, int sortOrder) {
        SystemDictTypeEntity existing = dictTypeMapper.selectOne(new LambdaQueryWrapper<SystemDictTypeEntity>()
                .eq(SystemDictTypeEntity::getDictCode, dictCode)
                .last("limit 1"));
        if (existing != null) {
            boolean changed = false;
            if (!dictName.equals(existing.getDictName())) {
                existing.setDictName(dictName);
                changed = true;
            }
            if (existing.getRemark() == null || existing.getRemark().isBlank()) {
                existing.setRemark(remark);
                changed = true;
            }
            if (existing.getSortOrder() == null || existing.getSortOrder() != sortOrder) {
                existing.setSortOrder(sortOrder);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                existing.setEnabled(true);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                dictTypeMapper.updateById(existing);
            }
            return existing;
        }
        SystemDictTypeEntity type = new SystemDictTypeEntity();
        type.setDictCode(dictCode);
        type.setDictName(dictName);
        type.setRemark(remark);
        type.setEnabled(true);
        type.setSortOrder(sortOrder);
        dictTypeMapper.insert(type);
        return type;
    }

    private void ensureDictItem(
            SystemDictTypeEntity type,
            String itemLabel,
            String itemValue,
            String color,
            int sortOrder
    ) {
        SystemDictItemEntity existing = dictItemMapper.selectOne(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getDictCode, type.getDictCode())
                .eq(SystemDictItemEntity::getItemValue, itemValue)
                .last("limit 1"));
        if (existing != null) {
            boolean changed = false;
            if (!type.getId().equals(existing.getTypeId())) {
                existing.setTypeId(type.getId());
                changed = true;
            }
            if (!itemLabel.equals(existing.getItemLabel())) {
                existing.setItemLabel(itemLabel);
                changed = true;
            }
            if (!java.util.Objects.equals(color, existing.getColor())) {
                existing.setColor(color);
                changed = true;
            }
            if (existing.getSortOrder() == null || existing.getSortOrder() != sortOrder) {
                existing.setSortOrder(sortOrder);
                changed = true;
            }
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                existing.setEnabled(true);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                dictItemMapper.updateById(existing);
            }
            return;
        }
        SystemDictItemEntity item = new SystemDictItemEntity();
        item.setTypeId(type.getId());
        item.setDictCode(type.getDictCode());
        item.setItemLabel(itemLabel);
        item.setItemValue(itemValue);
        item.setColor(color);
        item.setEnabled(true);
        item.setSortOrder(sortOrder);
        dictItemMapper.insert(item);
    }

    private void disableDictItem(String dictCode, String itemValue) {
        SystemDictItemEntity existing = dictItemMapper.selectOne(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getDictCode, dictCode)
                .eq(SystemDictItemEntity::getItemValue, itemValue)
                .last("limit 1"));
        if (existing == null || !Boolean.TRUE.equals(existing.getEnabled())) {
            return;
        }
        existing.setEnabled(false);
        existing.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(existing);
    }
}
