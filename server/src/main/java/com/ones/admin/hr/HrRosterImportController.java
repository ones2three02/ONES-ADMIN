package com.ones.admin.hr;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.hr.dto.HrRosterImportBatchQuery;
import com.ones.admin.hr.dto.HrRosterImportBatchResponse;
import com.ones.admin.hr.dto.HrRosterImportErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/hr/roster-import")
@Tag(name = "HRMS-花名册导入")
@ApiResourceMetadata(
        owner = "人力平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.58",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class HrRosterImportController {

    private final HrRosterImportService rosterImportService;

    public HrRosterImportController(HrRosterImportService rosterImportService) {
        this.rosterImportService = rosterImportService;
    }

    @GetMapping("/template")
    @SaCheckPermission("hr:roster:import")
    @Operation(operationId = "HrRosterImportController_downloadTemplate", summary = "下载花名册导入模板")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = rosterImportService.templateCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("ones-hr-roster-template.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }

    @PostMapping("/batches")
    @SaCheckPermission("hr:roster:import")
    @Operation(operationId = "HrRosterImportController_importRoster", summary = "上传并导入花名册")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<HrRosterImportBatchResponse> importRoster(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return ApiResult.ok(rosterImportService.importRoster(file));
    }

    @GetMapping("/batches")
    @SaCheckPermission("hr:roster:list")
    @Operation(operationId = "HrRosterImportController_listBatches", summary = "查询花名册导入批次")
    public ApiResult<PageResult<HrRosterImportBatchResponse>> listBatches(@Valid HrRosterImportBatchQuery query) {
        return ApiResult.ok(rosterImportService.listBatches(query));
    }

    @GetMapping("/batches/{id}")
    @SaCheckPermission("hr:roster:list")
    @Operation(operationId = "HrRosterImportController_getBatch", summary = "查询花名册导入批次详情")
    public ApiResult<HrRosterImportBatchResponse> getBatch(@PathVariable Long id) {
        return ApiResult.ok(rosterImportService.getBatch(id));
    }

    @GetMapping("/batches/{id}/errors")
    @SaCheckPermission("hr:roster:list")
    @Operation(operationId = "HrRosterImportController_listErrors", summary = "查询花名册导入错误行")
    public ApiResult<List<HrRosterImportErrorResponse>> listErrors(@PathVariable Long id) {
        return ApiResult.ok(rosterImportService.listErrors(id));
    }
}
