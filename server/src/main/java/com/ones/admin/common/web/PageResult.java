package com.ones.admin.common.web;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(
        long pageNum,
        long pageSize,
        long total,
        long pages,
        List<T> list,
        boolean empty
) {

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                list,
                list == null || list.isEmpty()
        );
    }
}
