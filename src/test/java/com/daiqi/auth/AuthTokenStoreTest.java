package com.daiqi.auth;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenStoreTest {

    @Test
    void tokenCanBeReadAndRemoved() {
        AuthTokenStore store = new AuthTokenStore(Ticker.systemTicker(), Duration.ofDays(7));

        store.put("token", 42L);

        assertThat(store.getUserId("token")).isEqualTo(42L);
        store.remove("token");
        assertThat(store.getUserId("token")).isNull();
    }

    @Test
    void tokenExpiresAfterSevenDays() {
        AtomicLong nanos = new AtomicLong();
        AuthTokenStore store = new AuthTokenStore(nanos::get, Duration.ofDays(7));
        store.put("token", 42L);

        nanos.set(Duration.ofDays(7).plusSeconds(1).toNanos());

        assertThat(store.getUserId("token")).isNull();
    }
}
