package com.zeromesh.service.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final String KEY_PREFIX  = "zeromesh:packet:";
    private static final Duration TTL       = Duration.ofHours(25); // 24h packet window + 1h buffer

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically claims a packetId using Redis SETNX.
     * Returns true if first time seen (proceed), false if already claimed (drop as duplicate).
     */
    public boolean claim(String packetId) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + packetId, "PROCESSING", TTL);
        return Boolean.TRUE.equals(success);
    }

    // Updates Redis status to SETTLED after successful DB commit
    public void markSettled(String packetId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + packetId, "SETTLED", TTL);
    }

    // Releases claim so packet can be retried (called on decryption failure or unknown accounts)
    public void release(String packetId) {
        redisTemplate.delete(KEY_PREFIX + packetId);
    }

    // Read-only status check — does not claim
    public String getStatus(String packetId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + packetId);
    }
}
