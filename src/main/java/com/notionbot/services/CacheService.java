package com.notionbot.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class CacheService<V> {
    private final Cache<String, V> cache;

    public CacheService(long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .build();
    }

    public void put(String key, V value) {
        cache.put(key, value);
    }

    public V get(String key) {
        return cache.getIfPresent(key);
    }

    public void remove(String key) {
        cache.invalidate(key);
    }
}
