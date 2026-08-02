package com.ones.admin.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityWebPropertiesTest {

    @Test
    void allowedOriginsIgnoreBlankValuesAndDuplicates() {
        SecurityWebProperties properties = new SecurityWebProperties();

        properties.setAllowedOrigins(Arrays.asList(" ", "https://admin.example.com ", null,
                "https://admin.example.com"));

        assertThat(properties.getAllowedOrigins()).containsExactly("https://admin.example.com");
    }
}
