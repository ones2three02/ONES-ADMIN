package com.ones.admin.hr;

import com.ones.admin.common.code.ErrorCode;

public enum HrErrorCode implements ErrorCode {

    POSITION_NOT_FOUND(4401, "岗位不存在"),
    POSITION_CODE_EXISTS(4402, "岗位编码已存在"),
    POSITION_NOT_AVAILABLE(4403, "岗位不存在或已停用"),

    JOB_GRADE_NOT_FOUND(4411, "职级不存在"),
    JOB_GRADE_CODE_EXISTS(4412, "职级编码已存在"),
    JOB_GRADE_NOT_AVAILABLE(4413, "职级不存在或已停用"),

    EMPLOYEE_NOT_FOUND(4421, "员工不存在"),
    EMPLOYEE_NO_EXISTS(4422, "员工编号已存在"),
    EMPLOYEE_DEPT_NOT_AVAILABLE(4423, "员工部门不存在或已停用"),
    EMPLOYEE_MANAGER_NOT_AVAILABLE(4424, "直属上级不存在或已离职"),
    EMPLOYEE_MANAGER_CANNOT_BE_SELF(4425, "直属上级不能是员工本人"),
    EMPLOYEE_ALREADY_RESIGNED(4426, "离职员工不能执行该操作"),
    EMPLOYEE_REGULARIZE_STATUS_INVALID(4427, "只有试用员工可以转正"),
    EMPLOYEE_RESIGN_DATE_INVALID(4428, "离职日期不能早于入职日期"),
    EMPLOYEE_TRANSFER_DATE_INVALID(4429, "调岗生效日期不能早于入职日期"),
    EMPLOYEE_REGULARIZE_DATE_INVALID(4430, "转正日期不能早于入职日期"),
    EMPLOYEE_DATA_SCOPE_DENIED(4436, "无权访问该员工数据"),
    EMPLOYEE_CONTRACT_NOT_FOUND(4431, "员工合同不存在"),
    EMPLOYEE_CONTRACT_NO_EXISTS(4432, "合同编号已存在"),
    EMPLOYEE_CONTRACT_DATE_INVALID(4433, "合同结束日期不能早于开始日期"),
    EMPLOYEE_CONTRACT_ALREADY_TERMINATED(4434, "员工合同已终止"),
    EMPLOYEE_CONTRACT_EXPIRING_DAYS_INVALID(4435, "合同到期查询天数必须在 0 到 365 之间"),
    EMPLOYEE_DOCUMENT_EXPIRING_DAYS_INVALID(4437, "员工资料到期查询天数必须在 0 到 365 之间"),
    ROSTER_IMPORT_FILE_EMPTY(4441, "花名册导入文件不能为空"),
    ROSTER_IMPORT_FILE_TYPE_INVALID(4442, "花名册导入仅支持 CSV 文件"),
    ROSTER_IMPORT_FILE_TOO_LARGE(4443, "花名册导入文件不能超过 2MB"),
    ROSTER_IMPORT_HEADER_INVALID(4444, "花名册导入模板表头不正确"),
    ROSTER_IMPORT_TOO_MANY_ROWS(4445, "花名册单次导入不能超过 1000 行"),
    ROSTER_IMPORT_BATCH_NOT_FOUND(4446, "花名册导入批次不存在"),
    HR_ENUM_INVALID(4491, "HRMS 枚举值不正确");

    private final int code;
    private final String message;

    HrErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
