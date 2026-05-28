package com.bot.service.common.config;

import lombok.Data;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class LocalCache {
    @Getter
    private static final LocalCache localCache = new LocalCache();
    private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

    private LocalCache() {}

    public void put(String key, Object value) { map.put(key, value); }
    public Object get(String key) { return map.get(key); }
    public void remove(String key) { map.remove(key); }
    public boolean containsKey(String key) { return map.containsKey(key); }
}