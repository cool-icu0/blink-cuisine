package com.example.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.dto.Result;
import com.example.entity.Shop;
import com.example.mapper.ShopMapper;
import com.example.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.utils.CacheClient;
import com.example.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.contants.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(
//                        CACHE_SHOP_KEY,
//                        id,
//                        Shop.class,
//                        this::getById,//全部写是id2->getById(id2)
//                        CACHE_SHOP_TTL,
//                        TimeUnit.MINUTES
//                );
        //互斥锁解决缓存击穿
//        Shop shop = cacheClient
//                .queryWithMutex(
//                        CACHE_SHOP_KEY,
//                        id,
//                        Shop.class,
//                        this::getById,
//                        CACHE_SHOP_TTL,
//                        TimeUnit.SECONDS
//                );
        //逻辑过期缓存击穿
        Shop shop = cacheClient
                .queryWithLogicalExpire(
                        CACHE_SHOP_KEY,
                        id,
                        Shop.class,
                        this::getById,
                        CACHE_SHOP_TTL,
                        TimeUnit.SECONDS
                );
        return Result.ok(shop);
    }
    //缓存击穿的加锁方法
//    public Shop queryWithMutex(Long id){
//        //1、从redis查询商铺缓存
//        String key = CACHE_SHOP_KEY + id;
//        //2、判断是否存在
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shopJson)){
//            //存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中的是否是空值
//        if (shopJson !=null){
//            //返回一个错误信息
//            return null;
//        }
//        //4.实现缓存重构
//        //4.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            //4.2 判断是否获取成功
//            if (!isLock){
//                //4.3 失败，则休眠重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //4.4 成功，根据id查询数据库
//            shop = getById(id);
//            //5、判断是否存在，不存在返回错误
//            if (shop == null){
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //6、存在，写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (Exception e){
//            throw new RuntimeException(e);
//        }finally {
//            //7，释放互斥锁
//            unlock(lockKey);
//        }
//        //8、返回
//        return shop;
//    }

    //缓存击穿的逻辑过期方法
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//    public Shop queryWithLogicalExpire(Long id){
//        //1、从redis查询商铺缓存
//        String key = CACHE_SHOP_KEY + id;
//        //2、判断是否命中
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(shopJson)){
//            //3.存在，直接返回
//            return null;
//        }
//        //4.命中，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data,Shop.class);
//        //5、判断是否过期
//        LocalDateTime expireTime = redisData.getExpireTime();
//        if(expireTime.isAfter(LocalDateTime.now())) {
//            // 5.1.未过期，直接返回店铺信息
//            return shop;
//        }
//        //5.2 已过期，需要缓存重建
//        //6 缓存重建
//        //6.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        //6.2 判断是否获取锁成功
//        if (isLock){
//            //6.3 成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    //6.3.1 重建缓存
//                    this.saveShop2Redis(id,20L);
//                }catch (Exception e){
//                    throw new RuntimeException(e);
//                }finally {
//                    //6.3.2 释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//        //6.4 失败，返回过期的商铺信息
//        //7、返回
//        return shop;
//    }

//    //缓存穿透的方法
//    public Shop queryWithPassThrough(Long id){
//        //1、从redis查询商铺缓存
//        String key = CACHE_SHOP_KEY + id;
//        //2、判断是否存在
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shopJson)){
//            //存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中的是否是空值
//        if (shopJson !=null){
//            //返回一个错误信息
//            return null;
//        }
//        //4、不存在，根据id查询数据库
//        Shop shop = getById(id);
//        //5、判断是否存在，不存在返回错误
//        if (shop == null){
//            //将空值写入redis
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6、存在，写入redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //7、返回
//        return shop;
//    }

    //设置锁
//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    //取消锁
//    private void unlock(String key) {
//        stringRedisTemplate.delete(key);
//    }

//    public void saveShop2Redis(Long id, Long expireSeconds) {
//        //1、查询店铺数据
//        Shop shop = getById(id);
//        //2、封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //3、写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
//    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
