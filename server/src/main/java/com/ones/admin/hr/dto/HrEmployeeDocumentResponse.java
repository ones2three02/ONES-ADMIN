package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HrEmployeeDocumentResponse(
        Long id,
        Long documentId,
        Long employeeId,
        String employeeNo,
        String realName,
        String deptName,
        Long fileId,
        String documentType,
        LocalDate issueDate,
        LocalDate expireDate,
        boolean expired,
        boolean expiringSoon,
        String remark,
        String originalName,
        String storedName,
        String url,
        String contentType,
        String extension,
        Long sizeBytes,
        String storageType,
        String bucket,
        String businessType,
        String businessId,
        Long uploadedBy,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
    public HrEmployeeDocumentResponse withFile(
            String businessType,
            String businessId,
            String status,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new HrEmployeeDocumentResponse(
                id,
                documentId,
                employeeId,
                employeeNo,
                realName,
                deptName,
                fileId,
                documentType,
                issueDate,
                expireDate,
                expired,
                expiringSoon,
                remark,
                originalName,
                storedName,
                url,
                contentType,
                extension,
                sizeBytes,
                storageType,
                bucket,
                businessType,
                businessId,
                uploadedBy,
                status,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
}
