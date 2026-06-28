package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.system.dto.DeptResponse;
import com.ones.admin.system.dto.DeptSaveRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/dept")
public class DeptController {

    private final DeptManagementService deptManagementService;

    public DeptController(DeptManagementService deptManagementService) {
        this.deptManagementService = deptManagementService;
    }

    @GetMapping("/list")
    @SaCheckPermission("system:dept:list")
    public ApiResult<List<DeptResponse>> listDeptTree() {
        return ApiResult.ok(deptManagementService.listTree());
    }

    @PostMapping
    @SaCheckPermission("system:dept:create")
    public ApiResult<DeptResponse> createDept(@Valid @RequestBody DeptSaveRequest request) {
        return ApiResult.ok(deptManagementService.create(request));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("system:dept:update")
    public ApiResult<DeptResponse> updateDept(
            @PathVariable Long id,
            @Valid @RequestBody DeptSaveRequest request
    ) {
        return ApiResult.ok(deptManagementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:dept:delete")
    public ApiResult<Void> deleteDept(@PathVariable Long id) {
        deptManagementService.delete(id);
        return ApiResult.ok(null);
    }
}
