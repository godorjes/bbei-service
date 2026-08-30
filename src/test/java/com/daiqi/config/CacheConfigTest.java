package com.daiqi.config;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void cacheManagerProvidesExistingBusinessCaches() {
        CacheManager manager = new CacheConfig().cacheManager();

        assertThat(manager.getCacheNames()).containsExactlyInAnyOrderElementsOf(
                Arrays.asList("tags", "cards", "scenes"));
    }
}
