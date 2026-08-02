package com.ones.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.DictItemQuery;
import com.ones.admin.system.dto.DictItemResponse;
import com.ones.admin.system.dto.DictItemSaveRequest;
import com.ones.admin.system.dto.DictOptionResponse;
import com.ones.admin.system.dto.DictTypeQuery;
import com.ones.admin.system.dto.DictTypeResponse;
import com.ones.admin.system.dto.DictTypeSaveRequest;
import com.ones.admin.system.entity.SystemDictItemEntity;
import com.ones.admin.system.entity.SystemDictTypeEntity;
import com.ones.admin.system.mapper.SystemDictItemMapper;
import com.ones.admin.system.mapper.SystemDictTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DictManagementService {

    private final SystemDictTypeMapper dictTypeMapper;
    private final SystemDictItemMapper dictItemMapper;

    public DictManagementService(SystemDictTypeMapper dictTypeMapper, SystemDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    public PageResult<DictTypeResponse> listTypes(DictTypeQuery query) {
        IPage<SystemDictTypeEntity> page = dictTypeMapper.selectPage(
                query.toMyBatisPage(),
                buildTypeWrapper(query)
        );
        List<DictTypeResponse> records = page.getRecords().stream()
                .map(this::toTypeResponse)
                .toList();
        return PageResult.of(page, records);
    }

    public PageResult<DictItemResponse> listItems(DictItemQuery query) {
        IPage<SystemDictItemEntity> page = dictItemMapper.selectPage(
                query.toMyBatisPage(),
                buildItemWrapper(query)
        );
        List<DictItemResponse> records = page.getRecords().stream()
                .map(this::toItemResponse)
                .toList();
        return PageResult.of(page, records);
    }

    public List<DictOptionResponse> listEnabledOptions(String dictCode) {
        SystemDictTypeEntity type = getRequiredEnabledType(normalizeCode(dictCode));
        return dictItemMapper.selectList(new LambdaQueryWrapper<SystemDictItemEntity>()
                        .eq(SystemDictItemEntity::getTypeId, type.getId())
                        .eq(SystemDictItemEntity::getEnabled, true)
                        .orderByAsc(SystemDictItemEntity::getSortOrder)
                        .orderByAsc(SystemDictItemEntity::getId))
                .stream()
                .map(item -> new DictOptionResponse(
                        item.getItemLabel(),
                        item.getItemValue(),
                        item.getColor(),
                        item.getSortOrder()
                ))
                .toList();
    }

    @Transactional
    public DictTypeResponse createType(DictTypeSaveRequest request) {
        String dictCode = normalizeCode(request.dictCode());
        assertTypeCodeAvailable(dictCode, null);
        SystemDictTypeEntity type = new SystemDictTypeEntity();
        type.setDictCode(dictCode);
        fillType(type, request);
        dictTypeMapper.insert(type);
        return toTypeResponse(dictTypeMapper.selectById(type.getId()));
    }

    @Transactional
    public DictTypeResponse updateType(Long id, DictTypeSaveRequest request) {
        SystemDictTypeEntity type = getRequiredType(id);
        String oldCode = type.getDictCode();
        String dictCode = normalizeCode(request.dictCode());
        assertTypeCodeAvailable(dictCode, id);
        type.setDictCode(dictCode);
        fillType(type, request);
        type.setUpdatedAt(LocalDateTime.now());
        dictTypeMapper.updateById(type);
        if (!oldCode.equals(dictCode)) {
            syncItemDictCode(type.getId(), dictCode);
        }
        return toTypeResponse(dictTypeMapper.selectById(id));
    }

    @Transactional
    public void deleteType(Long id) {
        getRequiredType(id);
        Long itemCount = dictItemMapper.selectCount(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getTypeId, id));
        if (itemCount > 0) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_HAS_ITEMS);
        }
        dictTypeMapper.deleteById(id);
    }

    @Transactional
    public DictItemResponse createItem(DictItemSaveRequest request) {
        SystemDictTypeEntity type = resolveType(request.typeId(), request.dictCode());
        assertItemValueAvailable(type.getDictCode(), normalizeValue(request.itemValue()), null);
        SystemDictItemEntity item = new SystemDictItemEntity();
        item.setTypeId(type.getId());
        item.setDictCode(type.getDictCode());
        fillItem(item, request);
        dictItemMapper.insert(item);
        return toItemResponse(dictItemMapper.selectById(item.getId()));
    }

    @Transactional
    public DictItemResponse updateItem(Long id, DictItemSaveRequest request) {
        SystemDictItemEntity item = getRequiredItem(id);
        SystemDictTypeEntity type = resolveType(request.typeId() == null ? item.getTypeId() : request.typeId(),
                request.dictCode());
        String itemValue = normalizeValue(request.itemValue());
        assertItemValueAvailable(type.getDictCode(), itemValue, id);
        item.setTypeId(type.getId());
        item.setDictCode(type.getDictCode());
        fillItem(item, request);
        item.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(item);
        return toItemResponse(dictItemMapper.selectById(id));
    }

    @Transactional
    public void deleteItem(Long id) {
        getRequiredItem(id);
        dictItemMapper.deleteById(id);
    }

    private LambdaQueryWrapper<SystemDictTypeEntity> buildTypeWrapper(DictTypeQuery query) {
        LambdaQueryWrapper<SystemDictTypeEntity> wrapper = new LambdaQueryWrapper<SystemDictTypeEntity>()
                .orderByAsc(SystemDictTypeEntity::getSortOrder)
                .orderByAsc(SystemDictTypeEntity::getId);
        if (hasText(query.getDictCode())) {
            wrapper.like(SystemDictTypeEntity::getDictCode, query.getDictCode().trim());
        }
        if (hasText(query.getDictName())) {
            wrapper.like(SystemDictTypeEntity::getDictName, query.getDictName().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(SystemDictTypeEntity::getEnabled, query.getEnabled());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SystemDictItemEntity> buildItemWrapper(DictItemQuery query) {
        LambdaQueryWrapper<SystemDictItemEntity> wrapper = new LambdaQueryWrapper<SystemDictItemEntity>()
                .orderByAsc(SystemDictItemEntity::getSortOrder)
                .orderByAsc(SystemDictItemEntity::getId);
        if (query.getTypeId() != null) {
            wrapper.eq(SystemDictItemEntity::getTypeId, query.getTypeId());
        }
        if (hasText(query.getDictCode())) {
            wrapper.eq(SystemDictItemEntity::getDictCode, normalizeCode(query.getDictCode()));
        }
        if (hasText(query.getItemLabel())) {
            wrapper.like(SystemDictItemEntity::getItemLabel, query.getItemLabel().trim());
        }
        if (hasText(query.getItemValue())) {
            wrapper.like(SystemDictItemEntity::getItemValue, query.getItemValue().trim());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(SystemDictItemEntity::getEnabled, query.getEnabled());
        }
        return wrapper;
    }

    private void fillType(SystemDictTypeEntity type, DictTypeSaveRequest request) {
        type.setDictName(request.dictName().trim());
        type.setRemark(normalizeNullable(request.remark()));
        type.setEnabled(request.enabled() == null || request.enabled());
        type.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void fillItem(SystemDictItemEntity item, DictItemSaveRequest request) {
        item.setItemLabel(request.itemLabel().trim());
        item.setItemValue(normalizeValue(request.itemValue()));
        item.setColor(normalizeNullable(request.color()));
        item.setRemark(normalizeNullable(request.remark()));
        item.setEnabled(request.enabled() == null || request.enabled());
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private void syncItemDictCode(Long typeId, String dictCode) {
        List<SystemDictItemEntity> items = dictItemMapper.selectList(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getTypeId, typeId));
        for (SystemDictItemEntity item : items) {
            item.setDictCode(dictCode);
            item.setUpdatedAt(LocalDateTime.now());
            dictItemMapper.updateById(item);
        }
    }

    private SystemDictTypeEntity resolveType(Long typeId, String dictCode) {
        if (typeId != null) {
            return getRequiredType(typeId);
        }
        if (hasText(dictCode)) {
            return getRequiredType(normalizeCode(dictCode));
        }
        throw new BusinessException(SystemErrorCode.DICT_TYPE_REQUIRED);
    }

    private SystemDictTypeEntity getRequiredType(Long id) {
        SystemDictTypeEntity type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_NOT_FOUND);
        }
        return type;
    }

    private SystemDictTypeEntity getRequiredType(String dictCode) {
        SystemDictTypeEntity type = dictTypeMapper.selectOne(new LambdaQueryWrapper<SystemDictTypeEntity>()
                .eq(SystemDictTypeEntity::getDictCode, dictCode)
                .last("limit 1"));
        if (type == null) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_NOT_FOUND);
        }
        return type;
    }

    private SystemDictTypeEntity getRequiredEnabledType(String dictCode) {
        SystemDictTypeEntity type = getRequiredType(dictCode);
        if (!Boolean.TRUE.equals(type.getEnabled())) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_DISABLED);
        }
        return type;
    }

    private SystemDictItemEntity getRequiredItem(Long id) {
        SystemDictItemEntity item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(SystemErrorCode.DICT_ITEM_NOT_FOUND);
        }
        return item;
    }

    private void assertTypeCodeAvailable(String dictCode, Long exceptId) {
        LambdaQueryWrapper<SystemDictTypeEntity> wrapper = new LambdaQueryWrapper<SystemDictTypeEntity>()
                .eq(SystemDictTypeEntity::getDictCode, dictCode);
        if (exceptId != null) {
            wrapper.ne(SystemDictTypeEntity::getId, exceptId);
        }
        if (dictTypeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_CODE_EXISTS);
        }
    }

    private void assertItemValueAvailable(String dictCode, String itemValue, Long exceptId) {
        LambdaQueryWrapper<SystemDictItemEntity> wrapper = new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getDictCode, dictCode)
                .eq(SystemDictItemEntity::getItemValue, itemValue);
        if (exceptId != null) {
            wrapper.ne(SystemDictItemEntity::getId, exceptId);
        }
        if (dictItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(SystemErrorCode.DICT_ITEM_VALUE_EXISTS);
        }
    }

    private DictTypeResponse toTypeResponse(SystemDictTypeEntity type) {
        Long itemCount = dictItemMapper.selectCount(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getTypeId, type.getId()));
        return new DictTypeResponse(
                type.getId(),
                type.getDictCode(),
                type.getDictName(),
                type.getRemark(),
                Boolean.TRUE.equals(type.getEnabled()),
                type.getSortOrder(),
                itemCount,
                type.getCreatedAt(),
                type.getUpdatedAt()
        );
    }

    private DictItemResponse toItemResponse(SystemDictItemEntity item) {
        return new DictItemResponse(
                item.getId(),
                item.getTypeId(),
                item.getDictCode(),
                item.getItemLabel(),
                item.getItemValue(),
                item.getColor(),
                item.getRemark(),
                Boolean.TRUE.equals(item.getEnabled()),
                item.getSortOrder(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeValue(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}
