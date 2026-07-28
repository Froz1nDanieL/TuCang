package com.mushan.tucangbackend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PictureCacheService {

    private static final String VERSION_KEY = "tucang:picture:cache:version";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String getVersion() {
        String version = stringRedisTemplate.opsForValue().get(VERSION_KEY);
        return version == null ? "0" : version;
    }

    public void invalidate() {
        stringRedisTemplate.opsForValue().increment(VERSION_KEY);
    }
}
