package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.CsvExportUtils;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrRosterImportBatchQuery;
import com.ones.admin.hr.dto.HrRosterImportBatchResponse;
import com.ones.admin.hr.dto.HrRosterImportErrorResponse;
import com.ones.admin.hr.entity.HrRosterImportBatchEntity;
import com.ones.admin.hr.entity.HrRosterImportErrorEntity;
import com.ones.admin.hr.mapper.HrRosterImportBatchMapper;
import com.ones.admin.hr.mapper.HrRosterImportErrorMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class HrRosterImportService {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_ROW_COUNT = 1000;
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "employeeNo",
            "realName",
            "deptId",
            "positionId",
            "gradeId",
            "employmentType",
            "employmentStatus",
            "hireDate",
            "probationEndDate",
            "mobile",
            "email",
            "gender",
            "preferredName",
            "remark"
    );

    private final HrRosterImportBatchMapper batchMapper;
    private final HrRosterImportErrorMapper errorMapper;
    private final HrEmployeeService employeeService;
    private final ObjectMapper objectMapper;

    public HrRosterImportService(
            HrRosterImportBatchMapper batchMapper,
            HrRosterImportErrorMapper errorMapper,
            HrEmployeeService employeeService,
            ObjectMapper objectMapper
    ) {
        this.batchMapper = batchMapper;
        this.errorMapper = errorMapper;
        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
    }

    public String templateCsv() {
        return CsvExportUtils.row(TEMPLATE_HEADERS.toArray())
                + "\n"
                + CsvExportUtils.row(
                "E20260703001",
                "张三",
                "1",
                "",
                "",
                "FULL_TIME",
                "PROBATION",
                "2026-07-03",
                "2026-10-03",
                "13800138000",
                "zhangsan@ones.local",
                "MALE",
                "三三",
                "示例行，导入前请删除"
        )
                + "\n";
    }

    public HrRosterImportBatchResponse importRoster(MultipartFile file) throws IOException {
        validateFile(file);
        List<String> lines = file.getResource()
                .getContentAsString(StandardCharsets.UTF_8)
                .lines()
                .toList();
        if (lines.isEmpty()) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_FILE_EMPTY);
        }
        List<String> headers = parseCsvLine(lines.getFirst());
        if (!TEMPLATE_HEADERS.equals(headers)) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_HEADER_INVALID);
        }

        HrRosterImportBatchEntity batch = new HrRosterImportBatchEntity();
        batch.setBatchNo("HRI" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        batch.setFileName(safeFilename(file.getOriginalFilename()));
        batch.setStatus("PARSING");
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setFailedCount(0);
        batch.setCreatedBy(currentUserId());
        batchMapper.insert(batch);

        int total = 0;
        int success = 0;
        int failed = 0;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            total++;
            if (total > MAX_ROW_COUNT) {
                throw new BusinessException(HrErrorCode.ROSTER_IMPORT_TOO_MANY_ROWS);
            }
            Map<String, String> row = rowMap(headers, parseCsvLine(line));
            RowFailure validationFailure = validateRow(row);
            if (validationFailure != null) {
                failed++;
                insertError(batch.getId(), index + 1, row, validationFailure);
                continue;
            }
            try {
                employeeService.createEmployee(toCreateRequest(row));
                success++;
            } catch (BusinessException ex) {
                failed++;
                insertError(batch.getId(), index + 1, row, new RowFailure("row", ex.getMessage()));
            }
        }

        batch.setTotalCount(total);
        batch.setSuccessCount(success);
        batch.setFailedCount(failed);
        batch.setStatus(importStatus(total, success, failed));
        batch.setCompletedAt(LocalDateTime.now());
        batchMapper.updateById(batch);
        return toBatchResponse(batchMapper.selectById(batch.getId()));
    }

    public PageResult<HrRosterImportBatchResponse> listBatches(HrRosterImportBatchQuery query) {
        IPage<HrRosterImportBatchEntity> page = batchMapper.selectPage(
                query.toMyBatisPage(),
                new LambdaQueryWrapper<HrRosterImportBatchEntity>()
                        .orderByDesc(HrRosterImportBatchEntity::getCreatedAt)
                        .orderByDesc(HrRosterImportBatchEntity::getId)
        );
        return PageResult.of(page, page.getRecords().stream().map(this::toBatchResponse).toList());
    }

    public HrRosterImportBatchResponse getBatch(Long id) {
        return toBatchResponse(getRequiredBatch(id));
    }

    public List<HrRosterImportErrorResponse> listErrors(Long batchId) {
        getRequiredBatch(batchId);
        return errorMapper.selectList(new LambdaQueryWrapper<HrRosterImportErrorEntity>()
                        .eq(HrRosterImportErrorEntity::getBatchId, batchId)
                        .orderByAsc(HrRosterImportErrorEntity::getRowNumber)
                        .orderByAsc(HrRosterImportErrorEntity::getId))
                .stream()
                .map(this::toErrorResponse)
                .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_FILE_TOO_LARGE);
        }
        if (!"csv".equals(extensionOf(file.getOriginalFilename()))) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_FILE_TYPE_INVALID);
        }
    }

    private RowFailure validateRow(Map<String, String> row) {
        if (!hasText(row.get("employeeNo"))) {
            return new RowFailure("employeeNo", "员工编号不能为空");
        }
        if (!hasText(row.get("realName"))) {
            return new RowFailure("realName", "员工姓名不能为空");
        }
        if (!hasText(row.get("deptId"))) {
            return new RowFailure("deptId", "部门 ID 不能为空");
        }
        if (!hasText(row.get("hireDate"))) {
            return new RowFailure("hireDate", "入职日期不能为空");
        }
        RowFailure deptFailure = validateLong(row, "deptId", "部门 ID 必须是数字");
        if (deptFailure != null) {
            return deptFailure;
        }
        RowFailure positionFailure = validateOptionalLong(row, "positionId", "岗位 ID 必须是数字");
        if (positionFailure != null) {
            return positionFailure;
        }
        RowFailure gradeFailure = validateOptionalLong(row, "gradeId", "职级 ID 必须是数字");
        if (gradeFailure != null) {
            return gradeFailure;
        }
        RowFailure hireDateFailure = validateDate(row, "hireDate", "入职日期格式必须是 yyyy-MM-dd");
        if (hireDateFailure != null) {
            return hireDateFailure;
        }
        return validateOptionalDate(row, "probationEndDate", "试用期结束日期格式必须是 yyyy-MM-dd");
    }

    private RowFailure validateLong(Map<String, String> row, String fieldName, String message) {
        try {
            Long.parseLong(row.get(fieldName).trim());
            return null;
        } catch (NumberFormatException ex) {
            return new RowFailure(fieldName, message);
        }
    }

    private RowFailure validateOptionalLong(Map<String, String> row, String fieldName, String message) {
        if (!hasText(row.get(fieldName))) {
            return null;
        }
        return validateLong(row, fieldName, message);
    }

    private RowFailure validateDate(Map<String, String> row, String fieldName, String message) {
        try {
            LocalDate.parse(row.get(fieldName).trim());
            return null;
        } catch (DateTimeParseException ex) {
            return new RowFailure(fieldName, message);
        }
    }

    private RowFailure validateOptionalDate(Map<String, String> row, String fieldName, String message) {
        if (!hasText(row.get(fieldName))) {
            return null;
        }
        return validateDate(row, fieldName, message);
    }

    private HrEmployeeCreateRequest toCreateRequest(Map<String, String> row) {
        return new HrEmployeeCreateRequest(
                row.get("employeeNo"),
                row.get("realName"),
                nullable(row.get("preferredName")),
                nullable(row.get("gender")),
                nullable(row.get("mobile")),
                nullable(row.get("email")),
                null,
                null,
                Long.parseLong(row.get("deptId").trim()),
                parseOptionalLong(row.get("positionId")),
                parseOptionalLong(row.get("gradeId")),
                null,
                nullable(row.get("employmentType")),
                nullable(row.get("employmentStatus")),
                LocalDate.parse(row.get("hireDate").trim()),
                parseOptionalDate(row.get("probationEndDate")),
                nullable(row.get("remark"))
        );
    }

    private void insertError(Long batchId, int rowNumber, Map<String, String> row, RowFailure failure) {
        HrRosterImportErrorEntity error = new HrRosterImportErrorEntity();
        error.setBatchId(batchId);
        error.setRowNumber(rowNumber);
        error.setEmployeeNo(nullable(row.get("employeeNo")));
        error.setFieldName(failure.fieldName());
        error.setErrorMessage(failure.message());
        error.setRawJson(writeRawJson(row));
        errorMapper.insert(error);
    }

    private HrRosterImportBatchEntity getRequiredBatch(Long id) {
        HrRosterImportBatchEntity batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(HrErrorCode.ROSTER_IMPORT_BATCH_NOT_FOUND);
        }
        return batch;
    }

    private String importStatus(int total, int success, int failed) {
        if (total == 0 || (success == 0 && failed > 0)) {
            return "VALIDATION_FAILED";
        }
        if (success > 0 && failed > 0) {
            return "PARTIAL_SUCCESS";
        }
        return "SUCCESS";
    }

    private Map<String, String> rowMap(List<String> headers, List<String> values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < values.size() ? values.get(index) : "";
            row.put(headers.get(index), value == null ? "" : value.trim());
        }
        return row;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (currentChar == ',' && !quoted) {
                values.add(stripBom(current.toString()));
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        values.add(stripBom(current.toString()));
        return values;
    }

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private Long parseOptionalLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private LocalDate parseOptionalDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String writeRawJson(Map<String, String> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException ex) {
            return String.valueOf(row);
        }
    }

    private HrRosterImportBatchResponse toBatchResponse(HrRosterImportBatchEntity batch) {
        return new HrRosterImportBatchResponse(
                batch.getId(),
                batch.getBatchNo(),
                batch.getFileName(),
                batch.getStatus(),
                batch.getTotalCount(),
                batch.getSuccessCount(),
                batch.getFailedCount(),
                batch.getCreatedBy(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }

    private HrRosterImportErrorResponse toErrorResponse(HrRosterImportErrorEntity error) {
        return new HrRosterImportErrorResponse(
                error.getId(),
                error.getBatchId(),
                error.getRowNumber(),
                error.getEmployeeNo(),
                error.getFieldName(),
                error.getErrorMessage(),
                error.getRawJson(),
                error.getCreatedAt()
        );
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String safeFilename(String originalFilename) {
        if (!hasText(originalFilename)) {
            return "roster.csv";
        }
        return Path.of(originalFilename).getFileName().toString();
    }

    private String extensionOf(String originalFilename) {
        if (!hasText(originalFilename)) {
            return "";
        }
        String filename = safeFilename(originalFilename);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String nullable(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record RowFailure(String fieldName, String message) {
    }
}
