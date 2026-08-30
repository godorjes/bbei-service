package com.daiqi.aspect;

import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebLogAspectTest {

    @Test
    void requestLoggingNeverIncludesArgumentsOrResponsePayload() throws Throwable {
        Logger logger = (Logger) LoggerFactory.getLogger(WebLogAspect.class);
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getDeclaringTypeName()).thenReturn("com.daiqi.controller.AuthController");
        when(signature.getName()).thenReturn("login");
        when(signature.toShortString()).thenReturn("AuthController.login(..)");
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{"argument-secret"});
        when(point.proceed()).thenReturn("response-secret");

        try {
            assertThat(new WebLogAspect().doAround(point)).isEqualTo("response-secret");
            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertThat(messages).doesNotContain("argument-secret", "response-secret");
        } finally {
            RequestContextHolder.resetRequestAttributes();
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
        }
    }
}
