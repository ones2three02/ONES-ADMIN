package com.ones.admin.common.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiResourceMetadata {

    String owner() default "";

    String audience() default "";

    String sinceVersion() default "";

    ApiLifecycleStatus lifecycle() default ApiLifecycleStatus.UNSPECIFIED;

    ApiRiskLevel riskLevel() default ApiRiskLevel.UNSPECIFIED;

    String sunsetVersion() default "";

    String replacementApiKey() default "";
}
