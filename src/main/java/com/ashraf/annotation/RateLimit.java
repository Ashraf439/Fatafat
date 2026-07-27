package com.ashraf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 10;
    int timeWindow() default 60;                // Duration window size
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}