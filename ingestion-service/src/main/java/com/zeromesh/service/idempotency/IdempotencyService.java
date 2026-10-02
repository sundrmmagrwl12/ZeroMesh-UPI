package com.zeromesh.service.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX  = "zeromesh:packet:";
    private static final Duration TTL       = Duration.ofHours(25); // 24h packet window + 1h buffer

    private final RedisTemplate<String, String> redisTemplate;
    private final Map<String, String> localMemoryFallback = new ConcurrentHashMap<>();

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically claims a packetId using Redis SETNX.
     * Falls back to in-memory store if Redis is unavailable (e.g. standalone cloud demo).
     */
    public boolean claim(String packetId) {
        try {
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + packetId, "PROCESSING", TTL);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            return localMemoryFallback.putIfAbsent(KEY_PREFIX + packetId, "PROCESSING") == null;
        }
    }

    // Updates status to SETTLED after successful DB commit
    public void markSettled(String packetId) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + packetId, "SETTLED", TTL);
        } catch (Exception e) {
            localMemoryFallback.put(KEY_PREFIX + packetId, "SETTLED");
        }
    }

    // Releases claim so packet can be retried
    public void release(String packetId) {
        try {
            redisTemplate.delete(KEY_PREFIX + packetId);
        } catch (Exception e) {
            localMemoryFallback.remove(KEY_PREFIX + packetId);
        }
    }

    // Read-only status check
    public String getStatus(String packetId) {
        try {
            return redisTemplate.opsForValue().get(KEY_PREFIX + packetId);
        } catch (Exception e) {
            return localMemoryFallback.get(KEY_PREFIX + packetId);
        }
    }
}
