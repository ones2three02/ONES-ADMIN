package com.ones.admin.system;

import com.ones.admin.common.code.ErrorCode;

public enum SystemErrorCode implements ErrorCode {

    USER_NOT_FOUND(4301, "用户不存在"),
    USERNAME_EXISTS(4302, "用户名已存在"),
    USER_ROLE_REQUIRED(4303, "用户至少需要分配一个角色"),
    USER_ROLE_NOT_AVAILABLE(4304, "角色不存在或已停用"),
    USER_DEPT_NOT_AVAILABLE(4305, "部门不存在或已停用"),
    DEFAULT_ADMIN_CANNOT_DISABLE(4306, "默认管理员不能停用"),
    DEFAULT_ADMIN_ROLE_REQUIRED(4307, "默认管理员必须保留超级管理员角色"),
    DEFAULT_ADMIN_CANNOT_DELETE(4308, "默认管理员不能删除"),

    ROLE_NOT_FOUND(4311, "角色不存在"),
    ROLE_CODE_EXISTS(4312, "角色编码已存在"),
    ROLE_CODE_REQUIRED(4313, "角色编码不能为空"),
    ROLE_NAME_REQUIRED(4314, "角色名称不能为空"),
    ROLE_ASSIGNED_TO_USER(4315, "角色已分配给用户，不能删除"),
    SUPER_ADMIN_CODE_LOCKED(4316, "超级管理员角色编码不能修改"),
    SUPER_ADMIN_CANNOT_DISABLE(4317, "超级管理员角色不能停用"),
    SUPER_ADMIN_PERMISSION_LOCKED(4318, "超级管理员权限不能修改"),
    SUPER_ADMIN_CANNOT_DELETE(4319, "超级管理员角色不能删除"),
    ROLE_MENU_PERMISSION_NOT_FOUND(4320, "菜单权限不存在"),

    MENU_NOT_FOUND(4331, "菜单不存在"),
    MENU_CHILD_EXISTS(4332, "存在下级菜单，不能删除"),
    MENU_PARENT_NOT_FOUND(4333, "上级菜单不存在"),
    MENU_PARENT_CANNOT_BE_BUTTON(4334, "按钮不能作为上级菜单"),
    MENU_PARENT_CANNOT_BE_DESCENDANT(4335, "上级菜单不能选择自己或自己的下级"),
    MENU_NAME_EXISTS(4336, "菜单名称已存在"),
    MENU_PATH_EXISTS(4337, "菜单路径已存在"),
    MENU_TYPE_REQUIRED(4338, "菜单类型不能为空"),
    MENU_TYPE_INVALID(4339, "菜单类型不正确"),
    MENU_PATH_REQUIRED(4340, "菜单路径不能为空"),

    DEPT_NOT_FOUND(4351, "部门不存在"),
    DEPT_CHILD_EXISTS(4352, "存在下级部门，不能删除"),
    DEPT_USER_EXISTS(4353, "部门下存在用户，不能删除"),
    DEPT_PARENT_NOT_FOUND(4354, "上级部门不存在"),
    DEPT_PARENT_CANNOT_BE_DESCENDANT(4355, "上级部门不能选择自己或自己的下级"),

    DICT_TYPE_NOT_FOUND(4361, "字典类型不存在"),
    DICT_TYPE_CODE_EXISTS(4362, "字典编码已存在"),
    DICT_TYPE_HAS_ITEMS(4363, "字典类型下存在字典项，不能删除"),
    DICT_TYPE_REQUIRED(4364, "字典类型不能为空"),
    DICT_TYPE_DISABLED(4365, "字典类型已停用"),
    DICT_ITEM_NOT_FOUND(4366, "字典项不存在"),
    DICT_ITEM_VALUE_EXISTS(4367, "同一字典下的字典值已存在"),

    FILE_EMPTY(4101, "上传文件不能为空"),
    FILE_EXTENSION_NOT_ALLOWED(4102, "不支持的文件类型"),
    FILE_TOO_LARGE(4103, "上传文件大小超过限制"),
    FILE_STORAGE_PATH_INVALID(4104, "文件存储路径不正确"),
    FILE_STORAGE_FAILED(4105, "文件存储失败");

    private final int code;
    private final String message;

    SystemErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
