package com.helper.gateway.utils;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisServices {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisServices(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 获取 Hash 的某个字段
     */
    public Mono<Object> hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 设置 Hash 字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long time) {
        if (time > 0) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, Duration.ofSeconds(time)));
        }
        return false;
    }

}