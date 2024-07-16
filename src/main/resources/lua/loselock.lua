---
--- Created by Cool.
--- DateTime: 2024/7/16 下午2:05
---
local key = KEYS[1] -- 锁的key
local threadId = ARGV[1] -- 线程id
local releaseTime = ARGV[2] -- 释放锁的时间戳

--判断当前线程是否持有锁，是否自己持有锁
if(redis.call('hexists', key, threadId) == 0) then
    return nil; --如果不是自己的锁，直接返回
end;
-- 是自己的锁，则重入次数-1
local count = redis.call('hincrby', key, threadId, '-1')
-- 判断是否重入次数是否为0
if(count > 0) then
   -- 大于0说明不能释放锁，重置有效期直接返回
   redis.call('expire', key, releaseTime)
   return nil;
else
    -- 重入次数为0，说明可以释放锁
    redis.call('del', key)
    return nil;
end;