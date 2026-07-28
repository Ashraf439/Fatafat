package com.ashraf.aop;

import com.ashraf.annotation.RateLimit;
import com.ashraf.exception.RateLimitExceedException;
import com.ashraf.service.RedisRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimiterService redisRateLimiterService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RedisRateLimiterService redisRateLimiterService) {
        this.redisRateLimiterService = redisRateLimiterService;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String ipAddress = request.getRemoteAddr();
        String methodName = joinPoint.getSignature().toShortString();

        String redisKey = "rate_limit:" + ipAddress + ":" + methodName;

        if (StringUtils.hasText(rateLimit.key())) {
            String resolvedKey = resolveKey(joinPoint, rateLimit.key());
            redisKey = redisKey + ":" + resolvedKey;
        }

        boolean allowed = redisRateLimiterService.isAllowed(
                redisKey,
                rateLimit.limit(),
                (int) rateLimit.timeUnit().toSeconds(rateLimit.timeWindow())
        );

        if (!allowed) throw new RateLimitExceedException("Too Many Requests. Please try again.");

        return joinPoint.proceed();
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Expression expression = expressionParser.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return value != null ? value.toString().toLowerCase() : "null";
    }
}