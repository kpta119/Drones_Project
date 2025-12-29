package com.example.drones.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache("operators", 60, 1000),
                buildCache("services", 1440, 100),
                buildCache("users", 30, 5000)
        ));
        return cacheManager;
    }


    private CaffeineCache buildCache(String name, long ttlInMinutes, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(ttlInMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build()
        );
    }
}
