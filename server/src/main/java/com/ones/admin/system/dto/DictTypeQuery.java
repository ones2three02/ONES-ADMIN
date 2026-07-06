package com.ones.admin.system.dto;

import com.ones.admin.common.web.PageQuery;

public class DictTypeQuery extends PageQuery {

    private String dictCode;
    private String dictName;
    private Boolean enabled;

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
