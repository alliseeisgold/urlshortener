package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String KEY_PREFIX = "url:";
    private static final String INACTIVE_SENTINEL = "__INACTIVE__";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    public void put(String shortCode, String originalUrl) {
        String key = buildKey(shortCode);
        redisTemplate.opsForValue().set(key, originalUrl, Duration.ofSeconds(cacheTtlSeconds));
        log.debug("Cached URL: {} -> {}", shortCode, originalUrl);
    }

    public void putInactive(String shortCode) {
        String key = buildKey(shortCode);
        redisTemplate.opsForValue().set(key, INACTIVE_SENTINEL, Duration.ofSeconds(60));
        log.debug("Cached INACTIVE for: {}", shortCode);
    }

    public Optional<String> get(String shortCode) {
        String key = buildKey(shortCode);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("Cache MISS for: {}", shortCode);
            return Optional.empty();
        }
        if (INACTIVE_SENTINEL.equals(value)) {
            log.debug("Cache HIT (inactive sentinel) for: {}", shortCode);
            return Optional.of(INACTIVE_SENTINEL);
        }
        log.debug("Cache HIT for: {}", shortCode);
        return Optional.of(value);
    }

    public void evict(String shortCode) {
        redisTemplate.delete(buildKey(shortCode));
        log.debug("Evicted cache for: {}", shortCode);
    }

    public boolean isInactiveSentinel(String value) {
        return INACTIVE_SENTINEL.equals(value);
    }

    private String buildKey(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
