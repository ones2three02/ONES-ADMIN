package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.dto.HrJobGradeResponse;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
import com.ones.admin.hr.entity.HrJobGradeEntity;
import com.ones.admin.hr.mapper.HrJobGradeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HrJobGradeService {

    private final HrJobGradeMapper jobGradeMapper;

    public HrJobGradeService(HrJobGradeMapper jobGradeMapper) {
        this.jobGradeMapper = jobGradeMapper;
    }

    public List<HrJobGradeResponse> listJobGrades() {
        return jobGradeMapper.selectList(new LambdaQueryWrapper<HrJobGradeEntity>()
                        .orderByAsc(HrJobGradeEntity::getGradeRank)
                        .orderByAsc(HrJobGradeEntity::getGradeCode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HrJobGradeResponse createJobGrade(HrJobGradeSaveRequest request) {
        String gradeCode = normalizeCode(request.gradeCode());
        assertGradeCodeAvailable(gradeCode, null);

        HrJobGradeEntity grade = new HrJobGradeEntity();
        grade.setGradeCode(gradeCode);
        fillGrade(grade, request);
        jobGradeMapper.insert(grade);
        return toResponse(jobGradeMapper.selectById(grade.getId()));
    }

    @Transactional
    public HrJobGradeResponse updateJobGrade(Long id, HrJobGradeSaveRequest request) {
        HrJobGradeEntity grade = getRequiredGrade(id);
        String gradeCode = normalizeCode(request.gradeCode());
        assertGradeCodeAvailable(gradeCode, id);
        grade.setGradeCode(gradeCode);
        fillGrade(grade, request);
        grade.setUpdatedAt(LocalDateTime.now());
        jobGradeMapper.updateById(grade);
        return toResponse(jobGradeMapper.selectById(id));
    }

    private void fillGrade(HrJobGradeEntity grade, HrJobGradeSaveRequest request) {
        grade.setGradeName(request.gradeName().trim());
        grade.setGradeRank(request.gradeRank());
        grade.setEnabled(request.enabled() == null || request.enabled());
    }

    private HrJobGradeEntity getRequiredGrade(Long id) {
        HrJobGradeEntity grade = jobGradeMapper.selectById(id);
        if (grade == null) {
            throw new BusinessException(HrErrorCode.JOB_GRADE_NOT_FOUND);
        }
        return grade;
    }

    private void assertGradeCodeAvailable(String gradeCode, Long exceptId) {
        LambdaQueryWrapper<HrJobGradeEntity> wrapper = new LambdaQueryWrapper<HrJobGradeEntity>()
                .eq(HrJobGradeEntity::getGradeCode, gradeCode);
        if (exceptId != null) {
            wrapper.ne(HrJobGradeEntity::getId, exceptId);
        }
        if (jobGradeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(HrErrorCode.JOB_GRADE_CODE_EXISTS);
        }
    }

    private HrJobGradeResponse toResponse(HrJobGradeEntity grade) {
        return new HrJobGradeResponse(
                grade.getId(),
                grade.getGradeCode(),
                grade.getGradeName(),
                grade.getGradeRank(),
                Boolean.TRUE.equals(grade.getEnabled()),
                grade.getCreatedAt(),
                grade.getUpdatedAt()
        );
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
