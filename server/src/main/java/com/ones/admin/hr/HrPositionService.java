package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.dto.HrPositionResponse;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import com.ones.admin.hr.entity.HrPositionEntity;
import com.ones.admin.hr.mapper.HrPositionMapper;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HrPositionService {

    private final HrPositionMapper positionMapper;
    private final SystemDeptMapper deptMapper;

    public HrPositionService(HrPositionMapper positionMapper, SystemDeptMapper deptMapper) {
        this.positionMapper = positionMapper;
        this.deptMapper = deptMapper;
    }

    public List<HrPositionResponse> listPositions() {
        return positionMapper.selectList(new LambdaQueryWrapper<HrPositionEntity>()
                        .orderByAsc(HrPositionEntity::getPositionCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HrPositionResponse createPosition(HrPositionSaveRequest request) {
        String positionCode = normalizeCode(request.positionCode());
        assertPositionCodeAvailable(positionCode, null);

        HrPositionEntity position = new HrPositionEntity();
        position.setPositionCode(positionCode);
        fillPosition(position, request);
        positionMapper.insert(position);
        return toResponse(positionMapper.selectById(position.getId()));
    }

    @Transactional
    public HrPositionResponse updatePosition(Long id, HrPositionSaveRequest request) {
        HrPositionEntity position = getRequiredPosition(id);
        String positionCode = normalizeCode(request.positionCode());
        assertPositionCodeAvailable(positionCode, id);
        position.setPositionCode(positionCode);
        fillPosition(position, request);
        position.setUpdatedAt(LocalDateTime.now());
        positionMapper.updateById(position);
        return toResponse(positionMapper.selectById(id));
    }

    private void fillPosition(HrPositionEntity position, HrPositionSaveRequest request) {
        position.setPositionName(request.positionName().trim());
        position.setDeptId(requireEnabledDept(request.deptId()));
        position.setDescription(normalizeNullable(request.description()));
        position.setEnabled(request.enabled() == null || request.enabled());
    }

    private HrPositionEntity getRequiredPosition(Long id) {
        HrPositionEntity position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException(HrErrorCode.POSITION_NOT_FOUND);
        }
        return position;
    }

    private void assertPositionCodeAvailable(String positionCode, Long exceptId) {
        LambdaQueryWrapper<HrPositionEntity> wrapper = new LambdaQueryWrapper<HrPositionEntity>()
                .eq(HrPositionEntity::getPositionCode, positionCode);
        if (exceptId != null) {
            wrapper.ne(HrPositionEntity::getId, exceptId);
        }
        if (positionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(HrErrorCode.POSITION_CODE_EXISTS);
        }
    }

    private Long requireEnabledDept(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SystemDeptEntity dept = deptMapper.selectById(deptId);
        if (dept == null || !Boolean.TRUE.equals(dept.getEnabled())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_DEPT_NOT_AVAILABLE);
        }
        return deptId;
    }

    private HrPositionResponse toResponse(HrPositionEntity position) {
        SystemDeptEntity dept = position.getDeptId() == null ? null : deptMapper.selectById(position.getDeptId());
        return new HrPositionResponse(
                position.getId(),
                position.getPositionCode(),
                position.getPositionName(),
                position.getDeptId(),
                dept == null ? null : dept.getName(),
                position.getDescription(),
                Boolean.TRUE.equals(position.getEnabled()),
                position.getCreatedAt(),
                position.getUpdatedAt()
        );
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
