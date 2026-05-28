package com.bot.service.common.config;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class LocalCache {
    private static final LocalCache localCache = new LocalCache();
    private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

    private LocalCache() {}

    public void set(String key, Object value) { map.put(key, value); }
    public Object get(String key) { return map.get(key); }
    public void del(String key) { map.remove(key); }
    public boolean exists(String key) { return map.containsKey(key); }
}