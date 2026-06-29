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

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(
                pageNum,
                pageSize,
                total,
                pages,
                list,
                list == null || list.isEmpty()
        );
    }
}
