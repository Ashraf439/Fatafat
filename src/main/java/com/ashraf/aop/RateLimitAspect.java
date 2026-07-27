package com.ashraf.aop;

import com.ashraf.annotation.RateLimit;
import com.ashraf.exception.RateLimitExceedException;
import com.ashraf.service.RedisRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimiterService redisRateLimiterService;


    public RateLimitAspect(RedisRateLimiterService redisRateLimiterService) {
        this.redisRateLimiterService = redisRateLimiterService;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String ipAddress = request.getRemoteAddr();
        String methodName = joinPoint.getSignature().toShortString();

        // Build a deterministic key based on client identity and target endpoint
        String redisKey = "rate_limit:" + ipAddress + ":" + methodName;

        boolean allowed = redisRateLimiterService.isAllowed(
                redisKey,
                rateLimit.limit(),
                (int) rateLimit.timeUnit().toSeconds(rateLimit.timeWindow())
        );

        if (!allowed) throw new RateLimitExceedException("Too Many Requests. Please try again.");

        return joinPoint.proceed();
    }
}
