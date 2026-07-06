package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.DictItemQuery;
import com.ones.admin.system.dto.DictItemResponse;
import com.ones.admin.system.dto.DictItemSaveRequest;
import com.ones.admin.system.dto.DictOptionResponse;
import com.ones.admin.system.dto.DictTypeQuery;
import com.ones.admin.system.dto.DictTypeResponse;
import com.ones.admin.system.dto.DictTypeSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/system/dicts")
@Tag(name = "系统管理-数据字典")
@ApiResourceMetadata(
        owner = "系统平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.73",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class DictController {

    private final DictManagementService dictManagementService;

    public DictController(DictManagementService dictManagementService) {
        this.dictManagementService = dictManagementService;
    }

    @GetMapping("/types")
    @SaCheckPermission("system:dict:type:list")
    @Operation(operationId = "DictController_listTypes", summary = "查询字典类型")
    public ApiResult<PageResult<DictTypeResponse>> listTypes(@Valid DictTypeQuery query) {
        return ApiResult.ok(dictManagementService.listTypes(query));
    }

    @PostMapping("/types")
    @SaCheckPermission("system:dict:type:create")
    @Operation(operationId = "DictController_createType", summary = "新增字典类型")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DictTypeResponse> createType(@Valid @RequestBody DictTypeSaveRequest request) {
        return ApiResult.ok(dictManagementService.createType(request));
    }

    @PutMapping("/types/{id}")
    @SaCheckPermission("system:dict:type:update")
    @Operation(operationId = "DictController_updateType", summary = "编辑字典类型")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DictTypeResponse> updateType(
            @PathVariable Long id,
            @Valid @RequestBody DictTypeSaveRequest request
    ) {
        return ApiResult.ok(dictManagementService.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @SaCheckPermission("system:dict:type:delete")
    @Operation(operationId = "DictController_deleteType", summary = "删除字典类型")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<Void> deleteType(@PathVariable Long id) {
        dictManagementService.deleteType(id);
        return ApiResult.ok(null);
    }

    @GetMapping("/items")
    @SaCheckPermission("system:dict:item:list")
    @Operation(operationId = "DictController_listItems", summary = "查询字典项")
    public ApiResult<PageResult<DictItemResponse>> listItems(@Valid DictItemQuery query) {
        return ApiResult.ok(dictManagementService.listItems(query));
    }

    @PostMapping("/items")
    @SaCheckPermission("system:dict:item:create")
    @Operation(operationId = "DictController_createItem", summary = "新增字典项")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DictItemResponse> createItem(@Valid @RequestBody DictItemSaveRequest request) {
        return ApiResult.ok(dictManagementService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @SaCheckPermission("system:dict:item:update")
    @Operation(operationId = "DictController_updateItem", summary = "编辑字典项")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<DictItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody DictItemSaveRequest request
    ) {
        return ApiResult.ok(dictManagementService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @SaCheckPermission("system:dict:item:delete")
    @Operation(operationId = "DictController_deleteItem", summary = "删除字典项")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<Void> deleteItem(@PathVariable Long id) {
        dictManagementService.deleteItem(id);
        return ApiResult.ok(null);
    }

    @GetMapping("/{dictCode}/options")
    @SaCheckLogin
    @ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "字典选项服务供登录用户在业务表单中复用，不授予字典维护权限")
    @Operation(operationId = "DictController_listOptions", summary = "查询字典选项")
    public ApiResult<List<DictOptionResponse>> listOptions(@PathVariable String dictCode) {
        return ApiResult.ok(dictManagementService.listEnabledOptions(dictCode));
    }
}
