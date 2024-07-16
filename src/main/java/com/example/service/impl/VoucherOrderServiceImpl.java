package com.example.service.impl;

import com.example.dto.Result;
import com.example.entity.VoucherOrder;
import com.example.mapper.VoucherOrderMapper;
import com.example.service.ISeckillVoucherService;
import com.example.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.utils.RedisIdWorker;
import com.example.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT= new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //通过开启一个单线程进行异步的处理
    private static final ExecutorService SECKILL_ORDER_EXECUTOR= Executors.newSingleThreadExecutor();
    //通过实现阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    //1.获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("处理订单异常",e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //1.获取用户
        Long userId = voucherOrder.getUserId();
        // 2.创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 3.尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 4.判断是否获得锁成功
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("不允许重复下单！");
            return;
        }
        try {
            //注意：由于是spring的事务是放在threadLocal中，此时的是多线程，事务会失效
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
    }

    private IVoucherOrderService proxy;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        //2.判断结果是否为0
        int r= result.intValue();
        if (r != 0){
            //2.1不为0，没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }

        //2.2为0，有购买资格，把下单信息保存到阻塞队列中
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4.用户id
        voucherOrder.setUserId(userId);
        // 2.5.代金券id
        voucherOrder.setVoucherId(voucherId);
        //2.6 创建阻塞队列，放入阻塞队列
        orderTasks.add(voucherOrder);
        //3.获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();
        // 4.返回订单id
        return Result.ok(orderId);
    }



//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //1.查询优惠卷
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        //2.判断秒杀是否开始
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
//            return Result.fail("秒杀尚未开始");
//        }
//        //3.判断秒杀是否结束
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
//            return Result.fail("秒杀已经结束");
//        }
//        //4.判断库存是否充足
//        if (seckillVoucher.getStock()< 1 ){
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//        //创建锁对象
////        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate,"order:" + userId);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        //获取锁
//        boolean isLock = lock.tryLock();
//        if (!isLock){
//            return Result.fail("不允许重复下单！");
//        }
//        try {
//            //获取代理对象（事务）
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }finally {
//            //释放锁
//            lock.unlock();
//        }
//    }
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        synchronized(userId.toString().intern()){
            // 5.1.查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder).count();
            // 5.2.判断是否存在
            if (count > 0) {
                // 用户已经购买过了
                log.info("用户已经购买过一次！");
                return;
            }

            // 6.扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1") // set stock = stock - 1
                    .eq("voucher_id", voucherOrder).gt("stock", 0) // where id = ? and stock > 0
                    .update();
            if (!success) {
                // 扣减失败
                log.info("库存不足！");
                return;
            }
            // 7.创建订单
            save(voucherOrder);
        }
    }
}
