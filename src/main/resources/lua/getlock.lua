---
--- Created by Cool.
--- DateTime: 2024/7/16 下午1:55
---
local key = KEYS[1] -- 锁的key
local threadId = ARGV[1] -- 线程id
local releaseTime = ARGV[2] -- 释放锁的时间戳

--判断是否存在
if(redis.call("exists", key) == 0) then
    -- 不存在,获取锁
    redis.call('hset', key, threadId,'1')
    -- 设置过期时间
    redis.call('expire', key, releaseTime)
    return 1;
end;
-- 锁已经存在，判断是否是自己的锁
if(redis.call('hexists', key, threadId) == 1) then
    -- 不存在，获取锁，重入次数+1
    redis.call('hincrby', key, threadId, '1')
    -- 设置过期时间
    redis.call('expire', key, releaseTime)
    return 1;
end;
return 0;
