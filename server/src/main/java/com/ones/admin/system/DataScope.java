package com.ones.admin.system;

import java.util.Locale;

public enum DataScope {

    ALL,
    DEPT_AND_CHILD,
    DEPT,
    SELF;

    public static DataScope from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("数据范围不能为空");
        }
        return DataScope.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean broaderThan(DataScope other) {
        return priority() < other.priority();
    }

    private int priority() {
        return switch (this) {
            case ALL -> 0;
            case DEPT_AND_CHILD -> 1;
            case DEPT -> 2;
            case SELF -> 3;
        };
    }
}
