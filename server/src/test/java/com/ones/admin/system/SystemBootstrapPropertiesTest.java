package com.ones.admin.system;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemBootstrapPropertiesTest {

    @Test
    void enabledBootstrapRequiresAdminPassword() {
        SystemBootstrapProperties properties = new SystemBootstrapProperties();
        properties.setEnabled(true);

        assertThatThrownBy(properties::requireAdminPassword)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ONES_BOOTSTRAP_ADMIN_PASSWORD");
    }
}
