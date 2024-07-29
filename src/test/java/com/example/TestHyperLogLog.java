package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
public class TestHyperLogLog {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Test
    void testHyperLogLog() {
        //准备数组，装用户数据
        String[] users = new String[1000];
        //数组脚标
        int index = 0;
        for (int i = 1; i < 1000000; i++) {
            users[index++] = "user_" + i;
            //每1000个用户数据，就计算一次
            if (i%1000 == 0){
                index=0;
                stringRedisTemplate.opsForHyperLogLog().add("hll1",users);
            }
        }
        //统计数据
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hll1");
        System.out.println("数据个数："+size);
    }
}
