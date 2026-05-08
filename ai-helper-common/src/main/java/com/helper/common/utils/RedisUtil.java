package com.helper.common.utils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /* ============================== Common ============================== */

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long time) {
        if (time > 0) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
        }
        return false;
    }

    /**
     * 获取过期时间，-1 为永不过期
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -1 : expire;
    }

    /**
     * 判断 key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 删除 key
     */
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(Arrays.asList(key));
            }
        }
    }

    /* ============================== String ============================== */

    /**
     * 普通缓存获取
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 带过期时间
     */
    public void set(String key, Object value, long time) {
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
    }

    /* ============================== Hash ============================== */

    /**
     * 获取 Hash 的某个字段
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取整个 Hash
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 设置 Hash 字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 设置整个 Hash
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 删除 Hash 字段
     */
    public void hDel(String key, String... fields) {
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /* ============================== Set ============================== */

    public void sAdd(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public void sRemove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    /* ============================== List ============================== */

    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    public Object lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /* ============================== ZSet ============================== */

    public void zAdd(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public void zRemove(String key, Object... values) {
        redisTemplate.opsForZSet().remove(key, values);
    }
}
