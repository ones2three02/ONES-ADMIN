package com.ones.admin.common;

import com.ones.admin.common.security.SensitiveDataMaskingUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskingUtilsTest {

    @Test
    void maskCommonHrSensitiveFields() {
        assertThat(SensitiveDataMaskingUtils.maskMobile("13800138000")).isEqualTo("138****8000");
        assertThat(SensitiveDataMaskingUtils.maskEmail("zhangsan@ones.local")).isEqualTo("z****n@ones.local");
        assertThat(SensitiveDataMaskingUtils.maskIdCard("110101199001011234")).isEqualTo("110****1234");
        assertThat(SensitiveDataMaskingUtils.maskMobile(null)).isNull();
        assertThat(SensitiveDataMaskingUtils.maskEmail("")).isNull();
    }
}
