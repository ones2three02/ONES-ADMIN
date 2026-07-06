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
        if (file == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        if (isUnbound(file)) {
            assertPermission("system:file:read");
            return;
        }
        FileBusinessAccessPolicy policy = businessAccessPolicies.stream()
                .filter(item -> item.supports(file))
                .findFirst()
                .orElse(null);
        if (policy != null) {
            policy.assertDownloadAllowed(file);
            return;
        }
        assertPermission("system:file:read");
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
