package com.daiqi.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {

    @Test
    void logsFinalResponseStatusWithoutQueryParameters() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpRequestLoggingFilter.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
            request.setQueryString("username=secret-user");
            MockHttpServletResponse response = new MockHttpServletResponse();

            new HttpRequestLoggingFilter().doFilter(request, response,
                    (servletRequest, servletResponse) -> ((MockHttpServletResponse) servletResponse).setStatus(201));

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .singleElement()
                    .asString()
                    .contains("POST /api/auth/register", "status=201")
                    .doesNotContain("secret-user");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
