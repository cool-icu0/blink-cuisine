package com.example.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private String name;

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String key = KEY_PREFIX + name;
        //获取线程id
        Long threadId = Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key,threadId+"",timeoutSec, TimeUnit.SECONDS);
        //自动拆箱
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        String key = KEY_PREFIX + name;
        stringRedisTemplate.delete(key);
    }
}
