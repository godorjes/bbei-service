package com.daiqi.aspect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class WebLogAspect {

    @Pointcut("execution(public * com.daiqi.controller..*.*(..))")
    public void webLog() {}

    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        HttpServletResponse response = attributes == null ? null : attributes.getResponse();

        try {
            return joinPoint.proceed();
        } finally {
            String method = request == null ? "N/A" : request.getMethod();
            String path = request == null ? "N/A" : request.getRequestURI();
            int status = response == null ? 0 : response.getStatus();
            log.debug("HTTP {} {} status={} handler={} durationMs={}",
                    method,
                    path,
                    status,
                    joinPoint.getSignature().toShortString(),
                    System.currentTimeMillis() - startedAt);
        }
    }
}
