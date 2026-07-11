package com.ones.admin.system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDataInitializerDisabledTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BootstrapTestConfiguration.class);

    @Test
    void disabledBootstrapDoesNotRegisterInitializer() {
        contextRunner
                .withPropertyValues("ones.bootstrap.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SystemDataInitializer.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({SystemDataInitializer.class, SystemBootstrapProperties.class})
    static class BootstrapTestConfiguration {
    }
}
