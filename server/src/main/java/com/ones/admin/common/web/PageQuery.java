package com.ones.admin.common.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageQuery {

    @Min(value = 1, message = "页码不能小于 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 200, message = "每页数量不能超过 200")
    private long pageSize = 20;

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public <T> Page<T> toMyBatisPage() {
        return new Page<>(pageNum, pageSize);
    }
}
