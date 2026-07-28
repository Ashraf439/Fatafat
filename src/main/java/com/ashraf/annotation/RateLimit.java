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
    int timeWindow() default 60;
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * Optional SpEL expression evaluated against the method's arguments,
     * e.g. "#req.email". When present, this value is folded into the Redis
     * key alongside IP, so limiting is scoped per (IP, target) rather than
     * per IP alone — closing the gap where one IP can spam N different
     * targets (e.g. N different victim emails) within the same window.
     * Blank (default) preserves the old IP-only behavior.
     */
    String key() default "";
}