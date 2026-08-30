package com.daiqi.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daiqi.auth.AuthTokenStore;
import com.daiqi.dto.LoginRequest;
import com.daiqi.dto.LoginResponse;
import com.daiqi.dto.RegisterRequest;
import com.daiqi.entity.User;
import com.daiqi.exception.BadRequestException;
import com.daiqi.exception.UnauthorizedException;
import com.daiqi.mapper.UserMapper;
import com.daiqi.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenStore authTokenStore;

    private static final long TOKEN_TTL_SECONDS = 7 * 24 * 3600; // 7 天

    @Override
    public LoginResponse register(RegisterRequest request) {
        String phone = normalizeOptionalPhone(request.getPhone());

        // 检查用户名是否已存在
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            throw new BadRequestException("用户名已被注册");
        }

        // 如果传了手机号，检查手机号是否已存在
        if (phone != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            User phoneUser = userMapper.selectOne(wrapper);
            if (phoneUser != null) {
                throw new BadRequestException("手机号已被注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("用户名或手机号已被注册");
        }

        log.info("新用户注册: userId={}", user.getId());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        log.info("用户登录: userId={}", user.getId());
        return buildLoginResponse(user);
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            authTokenStore.remove(token);
            log.info("用户登出");
        }
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond();

        authTokenStore.put(token, user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setExpireAt(expireAt);

        return response;
    }

    private String normalizeOptionalPhone(String phone) {
        return phone == null || phone.trim().isEmpty() ? null : phone;
    }
}
