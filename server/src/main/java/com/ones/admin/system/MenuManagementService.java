package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.menu.dto.MenuItem;
import com.ones.admin.system.dto.MenuMetaResponse;
import com.ones.admin.system.dto.MenuResponse;
import com.ones.admin.system.dto.MenuRouteResponse;
import com.ones.admin.system.dto.MenuSaveRequest;
import com.ones.admin.system.entity.SystemMenuEntity;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuManagementService {

    private static final Set<String> ROUTE_TYPES = Set.of("catalog", "menu", "embedded", "link");

    private final SystemMenuMapper menuMapper;
    private final SystemPermissionMapper permissionMapper;

    public MenuManagementService(SystemMenuMapper menuMapper, SystemPermissionMapper permissionMapper) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
    }

    public List<MenuResponse> listManagementTree() {
        List<SystemMenuEntity> menus = orderedMenus(false);
        Map<Long, List<SystemMenuEntity>> childrenByParentId = childrenByParentId(menus);
        return rootMenus(menus).stream()
                .map(menu -> toMenuResponse(menu, childrenByParentId))
                .toList();
    }

    public List<MenuItem> listCurrentMenuItems() {
        List<SystemMenuEntity> menus = orderedMenus(true);
        Map<Long, List<SystemMenuEntity>> childrenByParentId = childrenByParentId(menus);
        List<String> permissions = StpUtil.getPermissionList();
        return rootMenus(menus).stream()
                .map(menu -> toMenuItem(menu, childrenByParentId, permissions))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<MenuRouteResponse> listCurrentRoutes() {
        List<SystemMenuEntity> menus = orderedMenus(true);
        Map<Long, List<SystemMenuEntity>> childrenByParentId = childrenByParentId(menus);
        List<String> permissions = StpUtil.getPermissionList();
        return rootMenus(menus).stream()
                .map(menu -> toRoute(menu, childrenByParentId, permissions))
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean isNameExists(String name, Long exceptId) {
        LambdaQueryWrapper<SystemMenuEntity> wrapper = new LambdaQueryWrapper<SystemMenuEntity>()
                .eq(SystemMenuEntity::getName, name);
        if (exceptId != null) {
            wrapper.ne(SystemMenuEntity::getId, exceptId);
        }
        return menuMapper.selectCount(wrapper) > 0;
    }

    public boolean isPathExists(String path, Long exceptId) {
        LambdaQueryWrapper<SystemMenuEntity> wrapper = new LambdaQueryWrapper<SystemMenuEntity>()
                .eq(SystemMenuEntity::getPath, path);
        if (exceptId != null) {
            wrapper.ne(SystemMenuEntity::getId, exceptId);
        }
        return menuMapper.selectCount(wrapper) > 0;
    }

    @Transactional
    public MenuResponse create(MenuSaveRequest request) {
        SystemMenuEntity menu = new SystemMenuEntity();
        fillMenu(menu, request);
        assertUnique(menu.getName(), menu.getPath(), null);
        ensurePermission(menu.getAuthCode(), menu.getTitle());
        menuMapper.insert(menu);
        return toMenuResponse(menuMapper.selectById(menu.getId()), Map.of());
    }

    @Transactional
    public MenuResponse update(Long id, MenuSaveRequest request) {
        SystemMenuEntity menu = getRequiredMenu(id);
        fillMenu(menu, request);
        assertParentNotDescendant(id, menu.getParentId());
        menu.setUpdatedAt(LocalDateTime.now());
        assertUnique(menu.getName(), menu.getPath(), id);
        ensurePermission(menu.getAuthCode(), menu.getTitle());
        menuMapper.updateById(menu);
        return toMenuResponse(menuMapper.selectById(id), Map.of());
    }

    @Transactional
    public void delete(Long id) {
        getRequiredMenu(id);
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SystemMenuEntity>()
                .eq(SystemMenuEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在下级菜单，不能删除");
        }
        menuMapper.deleteById(id);
    }

    private void fillMenu(SystemMenuEntity menu, MenuSaveRequest request) {
        Long parentId = parseId(request.pid());
        assertParentExists(parentId);
        String type = normalizeType(request.type());
        String title = normalizeTitle(request);
        String path = normalizePath(request.path(), request.authCode(), parentId, type);
        menu.setParentId(parentId);
        menu.setName(request.name().trim());
        menu.setTitle(title);
        menu.setPath(path);
        menu.setComponent(normalizeNullable(request.component()));
        menu.setRedirect(normalizeNullable(request.redirect()));
        menu.setAuthCode(normalizeNullable(request.authCode()));
        menu.setIcon(request.meta() == null ? null : normalizeNullable(request.meta().icon()));
        menu.setType(type);
        menu.setSortOrder(request.meta() == null || request.meta().order() == null ? 0 : request.meta().order());
        menu.setEnabled(request.status() == null || request.status() == 1);
    }

    private void assertParentExists(Long parentId) {
        if (parentId == null) {
            return;
        }
        SystemMenuEntity parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("上级菜单不存在");
        }
        if ("button".equals(parent.getType())) {
            throw new BusinessException("按钮不能作为上级菜单");
        }
    }

    private void assertParentNotDescendant(Long currentId, Long parentId) {
        Long cursor = parentId;
        while (cursor != null) {
            if (Objects.equals(cursor, currentId)) {
                throw new BusinessException("上级菜单不能选择自己或自己的下级");
            }
            SystemMenuEntity parent = menuMapper.selectById(cursor);
            if (parent == null) {
                throw new BusinessException("上级菜单不存在");
            }
            cursor = parent.getParentId();
        }
    }

    private void ensurePermission(String code, String name) {
        String normalizedCode = normalizeNullable(code);
        if (normalizedCode == null) {
            return;
        }
        SystemPermissionEntity existing = permissionMapper.selectOne(new LambdaQueryWrapper<SystemPermissionEntity>()
                .eq(SystemPermissionEntity::getCode, normalizedCode)
                .last("limit 1"));
        if (existing != null) {
            if (name != null && !name.isBlank() && !Objects.equals(existing.getName(), name)) {
                existing.setName(name);
                permissionMapper.updateById(existing);
            }
            return;
        }
        SystemPermissionEntity permission = new SystemPermissionEntity();
        permission.setCode(normalizedCode);
        permission.setName(name);
        permissionMapper.insert(permission);
    }

    private void assertUnique(String name, String path, Long exceptId) {
        if (isNameExists(name, exceptId)) {
            throw new BusinessException("菜单名称已存在");
        }
        if (isPathExists(path, exceptId)) {
            throw new BusinessException("菜单路径已存在");
        }
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isBlank()) {
            throw new BusinessException("菜单类型不能为空");
        }
        String normalized = type.trim();
        if (!Set.of("catalog", "menu", "embedded", "link", "button").contains(normalized)) {
            throw new BusinessException("菜单类型不正确");
        }
        return normalized;
    }

    private String normalizeTitle(MenuSaveRequest request) {
        if (request.meta() == null || request.meta().title() == null || request.meta().title().trim().isBlank()) {
            return request.name().trim();
        }
        return request.meta().title().trim();
    }

    private String normalizePath(String path, String authCode, Long parentId, String type) {
        if (path != null && !path.trim().isBlank()) {
            return path.trim();
        }
        if (!"button".equals(type)) {
            throw new BusinessException("菜单路径不能为空");
        }
        String parentPath = parentId == null ? "/button" : getRequiredMenu(parentId).getPath();
        String suffix = authCode == null || authCode.trim().isBlank() ? "button" : authCode.trim();
        return parentPath + "#" + suffix;
    }

    private SystemMenuEntity getRequiredMenu(Long id) {
        SystemMenuEntity menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        return menu;
    }

    private List<SystemMenuEntity> orderedMenus(boolean enabledOnly) {
        LambdaQueryWrapper<SystemMenuEntity> wrapper = new LambdaQueryWrapper<SystemMenuEntity>()
                .orderByAsc(SystemMenuEntity::getSortOrder)
                .orderByAsc(SystemMenuEntity::getId);
        if (enabledOnly) {
            wrapper.eq(SystemMenuEntity::getEnabled, true);
        }
        return menuMapper.selectList(wrapper);
    }

    private List<SystemMenuEntity> rootMenus(List<SystemMenuEntity> menus) {
        return menus.stream()
                .filter(menu -> menu.getParentId() == null)
                .toList();
    }

    private Map<Long, List<SystemMenuEntity>> childrenByParentId(List<SystemMenuEntity> menus) {
        return menus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(SystemMenuEntity::getParentId));
    }

    private MenuResponse toMenuResponse(
            SystemMenuEntity menu,
            Map<Long, List<SystemMenuEntity>> childrenByParentId
    ) {
        List<MenuResponse> children = childrenByParentId
                .getOrDefault(menu.getId(), new ArrayList<>())
                .stream()
                .map(child -> toMenuResponse(child, childrenByParentId))
                .toList();
        return new MenuResponse(
                String.valueOf(menu.getId()),
                menu.getParentId() == null ? "0" : String.valueOf(menu.getParentId()),
                menu.getName(),
                menu.getPath(),
                menu.getComponent(),
                menu.getRedirect(),
                menu.getAuthCode(),
                menu.getType(),
                Boolean.TRUE.equals(menu.getEnabled()) ? 1 : 0,
                toMeta(menu),
                children
        );
    }

    private MenuItem toMenuItem(
            SystemMenuEntity menu,
            Map<Long, List<SystemMenuEntity>> childrenByParentId,
            List<String> permissions
    ) {
        List<MenuItem> children = childrenByParentId
                .getOrDefault(menu.getId(), List.of())
                .stream()
                .map(child -> toMenuItem(child, childrenByParentId, permissions))
                .filter(Objects::nonNull)
                .toList();
        boolean visible = hasPermission(menu, permissions) || !children.isEmpty();
        if (!visible || "button".equals(menu.getType())) {
            return null;
        }
        return new MenuItem(menu.getTitle(), menu.getPath(), menu.getIcon(), menu.getAuthCode(), children);
    }

    private MenuRouteResponse toRoute(
            SystemMenuEntity menu,
            Map<Long, List<SystemMenuEntity>> childrenByParentId,
            List<String> permissions
    ) {
        if (!ROUTE_TYPES.contains(menu.getType())) {
            return null;
        }
        List<MenuRouteResponse> children = childrenByParentId
                .getOrDefault(menu.getId(), List.of())
                .stream()
                .map(child -> toRoute(child, childrenByParentId, permissions))
                .filter(Objects::nonNull)
                .toList();
        boolean visible = hasPermission(menu, permissions) || !children.isEmpty();
        if (!visible) {
            return null;
        }
        if ("catalog".equals(menu.getType()) && children.isEmpty() && isBlank(menu.getComponent())) {
            return null;
        }
        return new MenuRouteResponse(
                menu.getName(),
                menu.getPath(),
                routeComponent(menu),
                menu.getRedirect(),
                toMeta(menu),
                children
        );
    }

    private String routeComponent(SystemMenuEntity menu) {
        if (!isBlank(menu.getComponent())) {
            return menu.getComponent();
        }
        return "catalog".equals(menu.getType()) ? "BasicLayout" : "/_core/fallback/not-found";
    }

    private MenuMetaResponse toMeta(SystemMenuEntity menu) {
        return new MenuMetaResponse(
                menu.getTitle(),
                menu.getIcon(),
                menu.getSortOrder(),
                false,
                false,
                false,
                false,
                "menu".equals(menu.getType()),
                Objects.equals(menu.getPath(), "/dashboard/overview")
        );
    }

    private boolean hasPermission(SystemMenuEntity menu, List<String> permissions) {
        return isBlank(menu.getAuthCode()) || permissions.contains(menu.getAuthCode());
    }

    private Long parseId(String id) {
        if (id == null || id.trim().isBlank() || "0".equals(id.trim())) {
            return null;
        }
        return Long.valueOf(id.trim());
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
