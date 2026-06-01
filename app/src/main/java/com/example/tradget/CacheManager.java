package com.example.tradget;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory + disk cache for improved performance.
 * Reduces database calls and network requests.
 */
public class CacheManager {

    private static CacheManager instance;
    private final Map<String, CachedItem<?>> memoryCache = new HashMap<>();
    private final SharedPreferences diskCache;
    private final Gson gson = new Gson();
    
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    private CacheManager(Context ctx) {
        diskCache = ctx.getSharedPreferences("app_cache", Context.MODE_PRIVATE);
    }

    public static synchronized CacheManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new CacheManager(ctx);
        }
        return instance;
    }

    /**
     * Stores a value in both memory and disk cache.
     */
    public <T> void put(String key, T value, Class<T> clazz) {
        // Memory cache
        memoryCache.put(key, new CachedItem<>(value, System.currentTimeMillis()));
        
        // Disk cache
        try {
            String json = gson.toJson(value);
            diskCache.edit().putString(key, json).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a value from cache (checks memory first, then disk).
     */
    public <T> T get(String key, Class<T> clazz) {
        // Check memory cache first
        CachedItem<?> item = memoryCache.get(key);
        if (item != null && !isExpired(item)) {
            return (T) item.value;
        }
        
        // Check disk cache
        try {
            String json = diskCache.getString(key, null);
            if (json != null) {
                T value = gson.fromJson(json, clazz);
                // Restore to memory cache
                memoryCache.put(key, new CachedItem<>(value, System.currentTimeMillis()));
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Checks if a cached item is expired.
     */
    private boolean isExpired(CachedItem<?> item) {
        return System.currentTimeMillis() - item.timestamp > CACHE_DURATION_MS;
    }

    /**
     * Clears all caches.
     */
    public void clearAll() {
        memoryCache.clear();
        diskCache.edit().clear().apply();
    }

    /**
     * Clears a specific cache entry.
     */
    public void remove(String key) {
        memoryCache.remove(key);
        diskCache.edit().remove(key).apply();
    }

    /**
     * Inner class to store cached values with timestamps.
     */
    private static class CachedItem<T> {
        T value;
        long timestamp;

        CachedItem(T value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
