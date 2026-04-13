package com.gateway.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 60000; // 1 minute

    public static byte[] get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_TTL) {
            return entry.data;
        }
        cache.remove(key);
        return null;
    }

    public static void put(String key, byte[] data) {
        cache.put(key, new CacheEntry(data, System.currentTimeMillis()));
    }

    private static class CacheEntry {
        byte[] data;
        long timestamp;

        CacheEntry(byte[] data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
