package com.daiqi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.daiqi.auth.AuthTokenStore;
import com.daiqi.dto.LoginRequest;
import com.daiqi.dto.RegisterRequest;
import com.daiqi.entity.User;
import com.daiqi.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

    @Test
    void persistsBlankOptionalPhoneAsNull() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("secret-password")).thenReturn("password-hash");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.<User>getArgument(0).setId(42L);
            return 1;
        });
        UserServiceImpl service = new UserServiceImpl(userMapper, passwordEncoder, new AuthTokenStore());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("private-user");
        request.setPassword("secret-password");
        request.setPhone("   ");

        service.register(request);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(user.capture());
        assertThat(user.getValue().getPhone()).isNull();
    }

    @Test
    void doesNotLogSubmittedUsernameAtInfo() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User user = new User();
        user.setId(42L);
        user.setUsername("secret-user");
        user.setPasswordHash("password-hash");
        when(userMapper.selectByUsername("secret-user")).thenReturn(user);
        when(passwordEncoder.matches("secret-password", "password-hash")).thenReturn(true);
        UserServiceImpl service = new UserServiceImpl(userMapper, passwordEncoder, new AuthTokenStore());

        Logger logger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);

        try {
            LoginRequest request = new LoginRequest();
            request.setUsername("secret-user");
            request.setPassword("secret-password");

            service.login(request);

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message).doesNotContain("secret-user"));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
