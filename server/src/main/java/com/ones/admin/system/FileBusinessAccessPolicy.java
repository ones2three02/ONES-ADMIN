package com.ones.admin.system;

import com.ones.admin.system.entity.SystemFileEntity;

public interface FileBusinessAccessPolicy {

    void assertDownloadAllowed(SystemFileEntity file);

    boolean supports(SystemFileEntity file);
}
