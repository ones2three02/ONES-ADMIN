package com.ones.admin.system.dto;

import com.ones.admin.common.web.PageQuery;

public class ApiResourceQuery extends PageQuery {

    private String method;
    private String path;
    private String module;
    private String permissionCode;
    private String handler;
    private String authType;
    private String owner;
    private String audience;
    private String lifecycle;
    private String riskLevel;
    private Boolean writeOperation;
    private Boolean permissionMissing;
    private Boolean deprecated;

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

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }
}
