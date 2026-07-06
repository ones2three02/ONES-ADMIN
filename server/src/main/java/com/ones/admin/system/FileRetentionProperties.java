package com.ones.admin.system;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "ones.file.retention")
public class FileRetentionProperties {

    @Min(1)
    private int deletedFileDays = 30;

    public int getDeletedFileDays() {
        return deletedFileDays;
    }

    public void setDeletedFileDays(int deletedFileDays) {
        this.deletedFileDays = deletedFileDays;
    }
}
