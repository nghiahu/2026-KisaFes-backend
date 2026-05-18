package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, String value, long ttlMillis) {
        stringRedisTemplate.opsForValue().set(key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void saveObject(String key, Object value, long ttlMillis) {
        redisTemplate.opsForValue().set(key, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}
