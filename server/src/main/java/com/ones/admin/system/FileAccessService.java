package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.entity.SystemFileEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileAccessService {

    private final List<FileBusinessAccessPolicy> businessAccessPolicies;

    public FileAccessService(List<FileBusinessAccessPolicy> businessAccessPolicies) {
        this.businessAccessPolicies = businessAccessPolicies;
    }

    public void assertDownloadAllowed(SystemFileEntity file) {
        assertAccessAllowed(file);
    }

    public void assertMetadataAllowed(SystemFileEntity file) {
        assertAccessAllowed(file);
    }

    public void assertDeleteAllowed(SystemFileEntity file) {
        if (file == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        if (isUnbound(file)) {
            assertGlobalFileReadPermissionOrOwner(file);
            return;
        }
        throw new BusinessException(SystemErrorCode.FILE_IN_USE);
    }

    private void assertAccessAllowed(SystemFileEntity file) {
        if (file == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        if (isUnbound(file)) {
            assertGlobalFileReadPermissionOrOwner(file);
            return;
        }
        FileBusinessAccessPolicy policy = resolveBusinessAccessPolicy(file);
        if (policy != null) {
            policy.assertDownloadAllowed(file);
            return;
        }
        assertPermission("system:file:read");
    }

    private FileBusinessAccessPolicy resolveBusinessAccessPolicy(SystemFileEntity file) {
        return businessAccessPolicies.stream()
                .filter(item -> item.supports(file))
                .findFirst()
                .orElse(null);
    }

    private void assertGlobalFileReadPermissionOrOwner(SystemFileEntity file) {
        if (StpUtil.hasPermission("system:file:read") || isUploadedByCurrentUser(file)) {
            return;
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    private boolean isUploadedByCurrentUser(SystemFileEntity file) {
        if (file.getUploadedBy() == null || !StpUtil.isLogin()) {
            return false;
        }
        return String.valueOf(file.getUploadedBy()).equals(String.valueOf(StpUtil.getLoginId()));
    }

    private boolean isUnbound(SystemFileEntity file) {
        return file.getBusinessType() == null && file.getBusinessId() == null;
    }

    private void assertPermission(String permission) {
        if (!StpUtil.hasPermission(permission)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }
}
