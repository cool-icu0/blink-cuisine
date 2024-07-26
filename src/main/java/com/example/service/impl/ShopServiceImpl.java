package com.example.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.Result;
import com.example.entity.Shop;
import com.example.mapper.ShopMapper;
import com.example.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.utils.CacheClient;
import com.example.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
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
        Shop shop = cacheClient
                .queryWithPassThrough(
                        CACHE_SHOP_KEY,
                        id,
                        Shop.class,
                        this::getById,//全部写是id2->getById(id2)
                        CACHE_SHOP_TTL,
                        TimeUnit.MINUTES
                );
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
//        Shop shop = cacheClient
//                .queryWithLogicalExpire(
//                        CACHE_SHOP_KEY,
//                        id,
//                        Shop.class,
//                        this::getById,
//                        CACHE_SHOP_TTL,
//                        TimeUnit.SECONDS
//                );
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}
