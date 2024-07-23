---
--- Created by Cool.
--- DateTime: 2024/7/16 下午5:10
---
-- 1.参数列表
-- 1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]
-- 2.数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId
-- 3.脚本业务
-- 3.1 判断库存是否充足
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end;
-- 3.2 判断用户是否重复下单
if(redis.call('sismember', orderKey, userId) == 1) then
    -- 用户重复下单，返回2
    return 2
end;
-- 3.3 库存充足，且用户没有重复下单，库存-1
redis.call('incrby', stockKey, -1)
-- 3.4 添加到set集合 下单（保存用户）sadd orderKey userId
redis.call('sadd', orderKey, userId)
-- 3.5.发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0