package com.ones.admin.system.dto;

import com.ones.admin.common.web.PageQuery;

public class ApiResourceManifestSnapshotQuery extends PageQuery {

    private String applicationVersion;
    private String checksum;

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
