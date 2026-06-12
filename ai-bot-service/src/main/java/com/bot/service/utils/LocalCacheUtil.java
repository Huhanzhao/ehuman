package com.bot.service.utils;

import lombok.Data;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

@Data
public class LocalCacheUtil {
    @Getter
    private static final LocalCacheUtil localCacheUtil = new LocalCacheUtil();
    private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

    private LocalCacheUtil() {}

    public void put(String key, Object value) { map.put(key, value); }
    public Object get(String key) { return map.get(key); }
    public void remove(String key) { map.remove(key); }
    public boolean containsKey(String key) { return map.containsKey(key); }
}