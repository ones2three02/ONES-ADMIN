package com.ones.admin.system.dto;

import com.ones.admin.common.web.PageQuery;

public class ApiResourceQuery extends PageQuery {

    private String method;
    private String path;
    private String module;
    private String permissionCode;
    private String authType;
    private Boolean writeOperation;
    private Boolean permissionMissing;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Boolean getWriteOperation() {
        return writeOperation;
    }

    public void setWriteOperation(Boolean writeOperation) {
        this.writeOperation = writeOperation;
    }

    public Boolean getPermissionMissing() {
        return permissionMissing;
    }

    public void setPermissionMissing(Boolean permissionMissing) {
        this.permissionMissing = permissionMissing;
    }
}
