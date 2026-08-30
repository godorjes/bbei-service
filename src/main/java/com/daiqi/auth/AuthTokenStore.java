package com.daiqi.auth;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenStore {

    private final Cache<String, Long> tokens;

    public AuthTokenStore() {
        this(Ticker.systemTicker(), Duration.ofDays(7));
    }

    AuthTokenStore(Ticker ticker, Duration ttl) {
        this.tokens = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfterWrite(ttl)
                .maximumSize(10_000)
                .build();
    }

    public void put(String token, Long userId) {
        tokens.put(token, userId);
    }

    public Long getUserId(String token) {
        return tokens.getIfPresent(token);
    }

    public void remove(String token) {
        tokens.invalidate(token);
    }
}
