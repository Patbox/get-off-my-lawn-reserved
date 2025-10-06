package draylar.goml.compat.webmap.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic caching system for PlayerRecord data to reduce API calls to Mojang.
 * Caches profile JSON responses with expiration.
 */
public class PlayerRecordCache {

    private static final long PROFILE_CACHE_EXPIRY_MS = 60 * 60 * 1000; // 1 hour
    private static final int MAX_CACHE_SIZE = 1000;

    private static final Map<UUID, CacheEntry<String>> profileCache = new ConcurrentHashMap<>();

    /**
     * Represents a cached entry with data and timestamp.
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long timestamp;

        public CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public T getData() {
            return data;
        }

        public boolean isExpired(long expiryMs) {
            return System.currentTimeMillis() - timestamp > expiryMs;
        }
    }

    /**
     * Gets cached profile JSON for the given UUID if not expired.
     * @param uuid the player UUID
     * @return the cached JSON string, or null if not cached or expired
     */
    public static String getProfile(UUID uuid) {
        CacheEntry<String> entry = profileCache.get(uuid);
        if (entry != null && !entry.isExpired(PROFILE_CACHE_EXPIRY_MS)) {
            return entry.getData();
        }
        // Remove expired entry
        if (entry != null) {
            profileCache.remove(uuid);
        }
        return null;
    }

    /**
     * Caches the profile JSON for the given UUID.
     * @param uuid the player UUID
     * @param json the JSON response from Mojang API
     */
    public static void putProfile(UUID uuid, String json) {
        if (profileCache.size() >= MAX_CACHE_SIZE) {
            clearExpiredProfiles();
        }
        profileCache.put(uuid, new CacheEntry<>(json));
    }

    /**
     * Clears expired profile cache entries.
     */
    public static void clearExpiredProfiles() {
        profileCache.entrySet().removeIf(entry -> entry.getValue().isExpired(PROFILE_CACHE_EXPIRY_MS));
    }

    /**
     * Clears all cache entries.
     */
    public static void clearAll() {
        profileCache.clear();
    }
}